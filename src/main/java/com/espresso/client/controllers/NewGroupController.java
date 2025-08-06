package com.espresso.client.controllers;

import com.espresso.client.MessageHandler;
import com.espresso.client.models.Group;
import com.espresso.client.models.GroupConversation;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the new group dialog.
 */
public class NewGroupController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(NewGroupController.class.getName());
    
    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private Button createButton;
    @FXML private Button cancelButton;
    
    private MessageHandler messageHandler;
    private MainChatController mainController;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up the create button
        createButton.setOnAction(this::handleCreate);
        
        // Set up the cancel button
        cancelButton.setOnAction(event -> closeDialog());
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
     * Handles the create button click.
     * 
     * @param event The action event
     */
    private void handleCreate(ActionEvent event) {
        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();
        
        // Validate input
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a group name.");
            return;
        }
        
        // Disable the create button
        createButton.setDisable(true);
        
        // Create the group
        CompletableFuture<String> createFuture = messageHandler.createGroup(name, description);
        
        createFuture.thenAccept(groupId -> {
            Platform.runLater(() -> {
                // Create a new group
                Group group = new Group(groupId, name, description, messageHandler.getCurrentUserId());
                
                // Create a new group conversation
                GroupConversation conversation = new GroupConversation(group);
                
                // Add the conversation to the main controller
                mainController.addGroupConversation(conversation);
                
                // Join the group chat
                messageHandler.joinGroupChat(groupId)
                        .exceptionally(ex -> {
                            LOGGER.log(Level.SEVERE, "Failed to join group chat", ex);
                            return null;
                        });
                
                // Close the dialog
                closeDialog();
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Create Group Failed", 
                        "Failed to create group: " + ex.getMessage());
                createButton.setDisable(false);
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
}