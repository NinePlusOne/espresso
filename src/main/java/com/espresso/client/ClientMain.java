package com.espresso.client;

import javafx.application.Application;

/**
 * Main entry point for the chat application.
 */
public class ClientMain {
    /**
     * Main method that launches the JavaFX application.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Launch the JavaFX application
        Application.launch(ChatApplication.class, args);
    }
}