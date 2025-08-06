package com.espresso.client.controllers;

import com.espresso.client.MessageHandler;
import com.espresso.client.models.DMConversation;
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
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for the new DM dialog.
 */
public class NewDMController implements Initializable {
    
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private ListView<User> resultsListView;
    @FXML private Button startChatButton;
    @FXML private Button cancelButton;
    
    private MessageHandler messageHandler;
    private MainChatController mainController;
    private final ObservableList<User> searchResults = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up the results list view
        resultsListView.setItems(searchResults);
        resultsListView.setCellFactory(param -> new UserCell());
        
        // Set up the search button
        searchButton.setOnAction(this::handleSearch);
        
        // Set up the start chat button
        startChatButton.setOnAction(this::handleStartChat);
        
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
        searchResults.clear();
        
        // Disable the search button
        searchButton.setDisable(true);
        
        // Search for users
        CompletableFuture<List<User>> searchFuture = messageHandler.searchUsers(query);
        
        searchFuture.thenAccept(users -> {
            Platform.runLater(() -> {
                // Filter out the current user
                users.removeIf(user -> user.getUserId().equals(messageHandler.getCurrentUserId()));
                
                // Add the results
                searchResults.addAll(users);
                
                // Enable the search button
                searchButton.setDisable(false);
                
                if (users.isEmpty()) {
                    showAlert(Alert.AlertType.INFORMATION, "No Results", "No users found matching your query.");
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Search Error", "Failed to search for users: " + ex.getMessage());
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
        User selectedUser = resultsListView.getSelectionModel().getSelectedItem();
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
}