package com.espresso.client.controllers;

import com.espresso.client.MessageHandler;
import com.espresso.client.models.Group;
import com.espresso.client.models.User;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the group chat dialog.
 */
public class GroupChatController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(GroupChatController.class.getName());
    
    @FXML private Label titleLabel;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private ListView<User> membersListView;
    @FXML private Button addButton;
    @FXML private Button removeButton;
    @FXML private Button leaveButton;
    
    private MessageHandler messageHandler;
    private Group group;
    private Stage stage;
    private final ObservableList<User> members = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up the members list view
        membersListView.setItems(members);
        
        // Set up the search button
        searchButton.setOnAction(this::handleSearch);
        
        // Set up the add button
        addButton.setOnAction(this::handleAddMember);
        
        // Set up the remove button
        removeButton.setOnAction(this::handleRemoveMember);
        
        // Set up the leave button
        leaveButton.setOnAction(this::handleLeaveGroup);
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
     * Sets the group.
     * 
     * @param group The group
     */
    public void setGroup(Group group) {
        this.group = group;
        
        // Update the title
        titleLabel.setText(group.getName());
        
        // Load members
        loadMembers();
    }
    
    /**
     * Sets the stage.
     * 
     * @param stage The stage
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    /**
     * Loads the group members.
     */
    private void loadMembers() {
        // Clear existing members
        members.clear();
        
        // Get the member IDs
        List<String> memberIds = group.getMemberIdsList();
        
        // Load each member
        for (String memberId : memberIds) {
            // In a real application, this would load user details from the server
            // For now, we'll just create a dummy user
            User user = new User(memberId, memberId);
            members.add(user);
        }
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
        
        // Search for users
        CompletableFuture<List<User>> searchFuture = messageHandler.searchUsers(query);
        
        searchFuture.thenAccept(users -> {
            Platform.runLater(() -> {
                // Filter out users who are already members
                users.removeIf(user -> group.isMember(user.getUserId()));
                
                if (users.isEmpty()) {
                    showAlert(Alert.AlertType.INFORMATION, "No Results", "No users found matching your query.");
                } else {
                    // Show the search results dialog
                    showSearchResultsDialog(users);
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Search Error", "Failed to search for users: " + ex.getMessage());
            });
            return null;
        });
    }
    
    /**
     * Shows the search results dialog.
     * 
     * @param users The list of users
     */
    private void showSearchResultsDialog(List<User> users) {
        // In a real application, this would show a dialog with the search results
        // For now, we'll just show an alert with the first user
        if (!users.isEmpty()) {
            User user = users.get(0);
            
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Add Member");
            alert.setHeaderText("Add " + user.getDisplayName() + " to the group?");
            alert.setContentText("User ID: " + user.getUserId());
            
            alert.showAndWait().ifPresent(result -> {
                if (result == javafx.scene.control.ButtonType.OK) {
                    // Add the user to the group
                    addMember(user);
                }
            });
        }
    }
    
    /**
     * Adds a member to the group.
     * 
     * @param user The user to add
     */
    private void addMember(User user) {
        // In a real application, this would send a request to the server
        // For now, we'll just add the user to the local group
        if (group.addMember(user.getUserId())) {
            // Add the user to the members list
            members.add(user);
            
            showAlert(Alert.AlertType.INFORMATION, "Member Added", 
                    user.getDisplayName() + " has been added to the group.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Add Member Failed", 
                    "Failed to add " + user.getDisplayName() + " to the group.");
        }
    }
    
    /**
     * Handles the add member button click.
     * 
     * @param event The action event
     */
    private void handleAddMember(ActionEvent event) {
        // Show the search dialog
        searchField.requestFocus();
    }
    
    /**
     * Handles the remove member button click.
     * 
     * @param event The action event
     */
    private void handleRemoveMember(ActionEvent event) {
        User selectedUser = membersListView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a member to remove.");
            return;
        }
        
        // Check if the user is the current user
        if (selectedUser.getUserId().equals(messageHandler.getCurrentUserId())) {
            showAlert(Alert.AlertType.WARNING, "Cannot Remove Self", 
                    "You cannot remove yourself from the group. Use the Leave button instead.");
            return;
        }
        
        // Check if the user is the owner
        if (group.isOwner(selectedUser.getUserId())) {
            showAlert(Alert.AlertType.WARNING, "Cannot Remove Owner", 
                    "You cannot remove the group owner.");
            return;
        }
        
        // Confirm removal
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Member");
        alert.setHeaderText("Remove " + selectedUser.getDisplayName() + " from the group?");
        alert.setContentText("This action cannot be undone.");
        
        alert.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                // Remove the user from the group
                removeMember(selectedUser);
            }
        });
    }
    
    /**
     * Removes a member from the group.
     * 
     * @param user The user to remove
     */
    private void removeMember(User user) {
        // In a real application, this would send a request to the server
        // For now, we'll just remove the user from the local group
        if (group.removeMember(user.getUserId())) {
            // Remove the user from the members list
            members.remove(user);
            
            showAlert(Alert.AlertType.INFORMATION, "Member Removed", 
                    user.getDisplayName() + " has been removed from the group.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Remove Member Failed", 
                    "Failed to remove " + user.getDisplayName() + " from the group.");
        }
    }
    
    /**
     * Handles the leave group button click.
     * 
     * @param event The action event
     */
    private void handleLeaveGroup(ActionEvent event) {
        // Check if the user is the owner
        if (group.isOwner(messageHandler.getCurrentUserId())) {
            showAlert(Alert.AlertType.WARNING, "Cannot Leave Group", 
                    "You are the owner of this group. You cannot leave it.");
            return;
        }
        
        // Confirm leaving
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Leave Group");
        alert.setHeaderText("Leave " + group.getName() + "?");
        alert.setContentText("You will no longer receive messages from this group.");
        
        alert.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                // Leave the group
                leaveGroup();
            }
        });
    }
    
    /**
     * Leaves the group.
     */
    private void leaveGroup() {
        // In a real application, this would send a request to the server
        // For now, we'll just leave the group locally
        CompletableFuture<Void> leaveFuture = messageHandler.leaveGroupChat(group.getGroupId());
        
        leaveFuture.thenRun(() -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.INFORMATION, "Left Group", 
                        "You have left " + group.getName() + ".");
                
                // Close the dialog
                if (stage != null) {
                    stage.close();
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Leave Group Failed", 
                        "Failed to leave the group: " + ex.getMessage());
            });
            return null;
        });
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
}