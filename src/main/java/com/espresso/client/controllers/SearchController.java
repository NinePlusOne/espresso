package com.espresso.client.controllers;

import com.espresso.client.MessageHandler;
import com.espresso.client.models.DMConversation;
import com.espresso.client.models.Group;
import com.espresso.client.models.GroupConversation;
import com.espresso.client.models.User;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for the search dialog.
 */
public class SearchController implements Initializable {
    
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private TabPane resultsTabPane;
    @FXML private Tab usersTab;
    @FXML private Tab groupsTab;
    @FXML private ListView<User> userResultsListView;
    @FXML private ListView<Map<String, String>> groupResultsListView;
    @FXML private Button startChatButton;
    @FXML private Button joinGroupButton;
    @FXML private Button cancelButton;
    
    private MessageHandler messageHandler;
    private MainChatController mainController;
    private final ObservableList<User> userResults = FXCollections.observableArrayList();
    private final ObservableList<Map<String, String>> groupResults = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up the user results list view
        userResultsListView.setItems(userResults);
        userResultsListView.setCellFactory(param -> new UserCell());
        
        // Set up the group results list view
        groupResultsListView.setItems(groupResults);
        groupResultsListView.setCellFactory(param -> new GroupCell());
        
        // Set up the search button
        searchButton.setOnAction(this::handleSearch);
        
        // Set up the start chat button
        startChatButton.setOnAction(this::handleStartChat);
        
        // Set up the join group button
        joinGroupButton.setOnAction(this::handleJoinGroup);
        
        // Set up the cancel button
        cancelButton.setOnAction(event -> closeDialog());
        
        // Set up the search field
        searchField.setOnAction(event -> handleSearch(event));
    }
    
    /**
     * Sets the message handler.
     * 
     * @param messageHandler The message handler
     */
    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }
    
    /**
     * Sets the main controller.
     * 
     * @param mainController The main controller
     */
    public void setMainController(MainChatController mainController) {
        this.mainController = mainController;
    }
    
    /**
     * Handles the search button click.
     * 
     * @param event The action event
     */
    private void handleSearch(ActionEvent event) {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            return;
        }
        
        // Clear existing results
        userResults.clear();
        groupResults.clear();
        
        // Disable the search button
        searchButton.setDisable(true);
        
        // Search for users
        CompletableFuture<List<User>> userSearchFuture = messageHandler.searchUsers(query);
        
        userSearchFuture.thenAccept(users -> {
            Platform.runLater(() -> {
                // Filter out the current user
                users.removeIf(user -> user.getUserId().equals(messageHandler.getCurrentUserId()));
                
                // Add the results
                userResults.addAll(users);
                
                // Enable the search button
                searchButton.setDisable(false);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Search Error", "Failed to search for users: " + ex.getMessage());
                searchButton.setDisable(false);
            });
            return null;
        });
        
        // Search for groups
        CompletableFuture<List<Map<String, String>>> groupSearchFuture = messageHandler.searchGroups(query);
        
        groupSearchFuture.thenAccept(groups -> {
            Platform.runLater(() -> {
                // Add the results
                groupResults.addAll(groups);
                
                // Enable the search button
                searchButton.setDisable(false);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Search Error", "Failed to search for groups: " + ex.getMessage());
                searchButton.setDisable(false);
            });
            return null;
        });
    }
    
    /**
     * Handles the start chat button click.
     * 
     * @param event The action event
     */
    private void handleStartChat(ActionEvent event) {
        User selectedUser = userResultsListView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user to chat with.");
            return;
        }
        
        // Create a new DM conversation
        DMConversation conversation = new DMConversation(
                messageHandler.getCurrentUserId(),
                selectedUser.getUserId()
        );
        
        // Add the conversation to the main controller
        mainController.addDMConversation(conversation);
        
        // Close the dialog
        closeDialog();
    }
    
    /**
     * Handles the join group button click.
     * 
     * @param event The action event
     */
    private void handleJoinGroup(ActionEvent event) {
        Map<String, String> selectedGroup = groupResultsListView.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a group to join.");
            return;
        }
        
        String groupId = selectedGroup.get("groupId");
        String name = selectedGroup.get("name");
        String description = selectedGroup.get("description");
        
        // Create a new group
        Group group = new Group(groupId, name, description, "unknown"); // Owner ID is unknown
        
        // Add the current user as a member
        group.addMember(messageHandler.getCurrentUserId());
        
        // Create a new group conversation
        GroupConversation conversation = new GroupConversation(group);
        
        // Join the group chat
        messageHandler.joinGroupChat(groupId)
                .thenRun(() -> {
                    Platform.runLater(() -> {
                        // Add the conversation to the main controller
                        mainController.addGroupConversation(conversation);
                        
                        // Close the dialog
                        closeDialog();
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Join Group Failed", 
                                "Failed to join group: " + ex.getMessage());
                    });
                    return null;
                });
    }
    
    /**
     * Closes the dialog.
     */
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Shows an alert dialog.
     * 
     * @param type The alert type
     * @param title The alert title
     * @param message The alert message
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Custom cell for users.
     */
    private class UserCell extends ListCell<User> {
        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            
            if (empty || user == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(user.getDisplayName() + " (" + user.getUserId() + ")");
            }
        }
    }
    
    /**
     * Custom cell for groups.
     */
    private class GroupCell extends ListCell<Map<String, String>> {
        @Override
        protected void updateItem(Map<String, String> group, boolean empty) {
            super.updateItem(group, empty);
            
            if (empty || group == null) {
                setText(null);
                setGraphic(null);
            } else {
                String name = group.get("name");
                String memberCount = group.get("memberCount");
                setText(name + " (" + memberCount + " members)");
            }
        }
    }
}