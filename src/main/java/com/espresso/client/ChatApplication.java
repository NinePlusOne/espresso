package com.espresso.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main JavaFX application class for the chat client.
 */
public class ChatApplication extends Application {
    private static final Logger LOGGER = Logger.getLogger(ChatApplication.class.getName());
    private static final String APP_TITLE = "Espresso Chat";
    private static final int MIN_WIDTH = 800;
    private static final int MIN_HEIGHT = 600;
    private static final int PREFERRED_WIDTH = 1000;
    private static final int PREFERRED_HEIGHT = 700;
    
    private MessageHandler messageHandler;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize the message handler
            messageHandler = new MessageHandler();
            
            // Load the login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set the message handler
            com.espresso.client.controllers.LoginController controller = loader.getController();
            controller.setMessageHandler(messageHandler);
            controller.setPrimaryStage(primaryStage);
            
            // Set up the scene
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/modern-theme.css").toExternalForm());
            
            // Set up the stage
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
            primaryStage.setWidth(PREFERRED_WIDTH);
            primaryStage.setHeight(PREFERRED_HEIGHT);
            
            // Set application icon if available
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app-icon.png")));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not load application icon", e);
            }
            
            // Show the stage
            primaryStage.show();
            
            // Set up close request handler
            primaryStage.setOnCloseRequest(event -> {
                if (messageHandler != null) {
                    messageHandler.shutdown();
                }
            });
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            showErrorAndExit("Failed to start application: " + e.getMessage());
        }
    }
    
    /**
     * Shows an error message and exits the application.
     * 
     * @param message The error message
     */
    private void showErrorAndExit(String message) {
        LOGGER.severe(message);
        Platform.exit();
    }
    
    @Override
    public void stop() {
        if (messageHandler != null) {
            messageHandler.shutdown();
        }
    }
}