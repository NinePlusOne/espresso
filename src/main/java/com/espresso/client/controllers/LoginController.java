package com.espresso.client.controllers;

import com.espresso.client.MessageHandler;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the login screen.
 */
public class LoginController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    
    @FXML private VBox loginPane;
    @FXML private VBox registerPane;
    @FXML private TextField loginUserIdField;
    @FXML private PasswordField loginPasswordField;
    @FXML private TextField registerUserIdField;
    @FXML private TextField registerDisplayNameField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField registerConfirmPasswordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private ToggleButton toggleModeButton;
    
    private MessageHandler messageHandler;
    private Stage primaryStage;
    private boolean isLoginMode = true;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up the toggle mode button
        toggleModeButton.setOnAction(this::toggleMode);
        
        // Initially show the login pane
        showLoginPane();
        
        // Set up the login button
        loginButton.setOnAction(this::handleLogin);
        
        // Set up the register button
        registerButton.setOnAction(this::handleRegister);
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
     * Sets the primary stage.
     * 
     * @param primaryStage The primary stage
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // Set the title now that we have the stage
        if (isLoginMode) {
            showLoginPane();
        } else {
            showRegisterPane();
        }
    }
    
    /**
     * Toggles between login and register mode.
     * 
     * @param event The action event
     */
    private void toggleMode(ActionEvent event) {
        isLoginMode = !isLoginMode;
        
        if (isLoginMode) {
            showLoginPane();
        } else {
            showRegisterPane();
        }
    }
    
    /**
     * Shows the login pane.
     */
    private void showLoginPane() {
        loginPane.setVisible(true);
        registerPane.setVisible(false);
        toggleModeButton.setText("Create Account");
        if (primaryStage != null) {
            primaryStage.setTitle("Espresso Chat - Login");
        }
    }
    
    /**
     * Shows the register pane.
     */
    private void showRegisterPane() {
        loginPane.setVisible(false);
        registerPane.setVisible(true);
        toggleModeButton.setText("Back to Login");
        if (primaryStage != null) {
            primaryStage.setTitle("Espresso Chat - Register");
        }
    }
    
    /**
     * Handles the login button click.
     * 
     * @param event The action event
     */
    private void handleLogin(ActionEvent event) {
        String userId = loginUserIdField.getText().trim();
        String password = loginPasswordField.getText();
        
        // Validate input
        if (userId.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Login Error", "Please enter both user ID and password.");
            return;
        }
        
        // Disable the login button
        loginButton.setDisable(true);
        
        // Attempt to login
        CompletableFuture<Boolean> loginFuture = messageHandler.login(userId, password);
        
        loginFuture.thenAccept(success -> {
            Platform.runLater(() -> {
                if (success) {
                    // Login successful, switch to the main chat screen
                    loadMainChatScreen();
                } else {
                    // Login failed
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid user ID or password.");
                    loginButton.setDisable(false);
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Login Error", "An error occurred: " + ex.getMessage());
                loginButton.setDisable(false);
            });
            return null;
        });
    }
    
    /**
     * Handles the register button click.
     * 
     * @param event The action event
     */
    private void handleRegister(ActionEvent event) {
        String userId = registerUserIdField.getText().trim();
        String displayName = registerDisplayNameField.getText().trim();
        String password = registerPasswordField.getText();
        String confirmPassword = registerConfirmPasswordField.getText();
        
        // Validate input
        if (userId.isEmpty() || displayName.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Registration Error", "Please fill in all fields.");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Registration Error", "Passwords do not match.");
            return;
        }
        
        // Disable the register button
        registerButton.setDisable(true);
        
        // Attempt to register
        CompletableFuture<Boolean> registerFuture = messageHandler.register(userId, password, displayName);
        
        registerFuture.thenAccept(success -> {
            Platform.runLater(() -> {
                if (success) {
                    // Registration successful, switch to login mode
                    showAlert(Alert.AlertType.INFORMATION, "Registration Successful", 
                            "Your account has been created. Please log in.");
                    showLoginPane();
                    loginUserIdField.setText(userId);
                    loginPasswordField.setText("");
                } else {
                    // Registration failed
                    showAlert(Alert.AlertType.ERROR, "Registration Failed", 
                            "Could not create account. User ID may already be taken.");
                }
                registerButton.setDisable(false);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Registration Error", "An error occurred: " + ex.getMessage());
                registerButton.setDisable(false);
            });
            return null;
        });
    }
    
    /**
     * Loads the main chat screen.
     */
    private void loadMainChatScreen() {
        try {
            // Load the main chat screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-chat.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set the message handler
            MainChatController controller = loader.getController();
            controller.setMessageHandler(messageHandler);
            controller.setPrimaryStage(primaryStage);
            controller.initializeAfterLogin();
            
            // Set up the scene
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/modern-theme.css").toExternalForm());
            
            // Set up the stage
            primaryStage.setTitle("Espresso Chat");
            primaryStage.setScene(scene);
            
            // Ensure the stage is showing
            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load main chat screen", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load main chat screen: " + e.getMessage());
        }
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