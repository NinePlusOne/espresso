package com.espresso.client.controllers;

import com.espresso.client.MessageHandler;
import com.espresso.client.models.DMConversation;
import com.espresso.client.models.Group;
import com.espresso.client.models.GroupConversation;
import com.espresso.client.models.Message;
import com.espresso.client.models.User;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the main chat screen.
 */
public class MainChatController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(MainChatController.class.getName());
    
    @FXML private BorderPane rootPane;
    @FXML private TabPane sidebarTabPane;
    @FXML private Tab dmTab;
    @FXML private Tab groupsTab;
    @FXML private ListView<DMConversation> dmListView;
    @FXML private ListView<GroupConversation> groupListView;
    @FXML private VBox chatArea;
    @FXML private Label chatHeaderLabel;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesContainer;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Button emojiButton;
    @FXML private Button newDmButton;
    @FXML private Button newGroupButton;
    @FXML private Button searchButton;
    @FXML private Button logoutButton;
    @FXML private MenuItem darkThemeMenuItem;
    @FXML private MenuItem lightThemeMenuItem;
    
    private MessageHandler messageHandler;
    private Stage primaryStage;
    private String currentUserId;
    private String currentChatId;
    private boolean isDarkTheme = true;
    
    private final ObservableList<DMConversation> dmConversations = FXCollections.observableArrayList();
    private final ObservableList<GroupConversation> groupConversations = FXCollections.observableArrayList();
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Group> groups = new HashMap<>();
    private final Map<String, Consumer<Message>> messageListeners = new HashMap<>();
    private final Map<String, Consumer<User>> userStatusListeners = new HashMap<>();
    private final Map<String, LocalDate> lastDateLabels = new HashMap<>();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up the DM list view
        dmListView.setItems(dmConversations);
        dmListView.setCellFactory(param -> new DMConversationCell());
        dmListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                openDMConversation(newVal);
            }
        });
        
        // Set up the group list view
        groupListView.setItems(groupConversations);
        groupListView.setCellFactory(param -> new GroupConversationCell());
        groupListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                openGroupConversation(newVal);
            }
        });
        
        // Set up the message input
        messageInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                sendMessage();
                event.consume();
            }
        });
        
        // Set up the send button
        sendButton.setOnAction(event -> sendMessage());
        
        // Set up the emoji button
        emojiButton.setOnAction(this::showEmojiPicker);
        
        // Set up the new DM button
        newDmButton.setOnAction(this::showNewDmDialog);
        
        // Set up the new group button
        newGroupButton.setOnAction(this::showNewGroupDialog);
        
        // Set up the search button
        searchButton.setOnAction(this::showSearchDialog);
        
        // Set up the logout button
        logoutButton.setOnAction(this::handleLogout);
        
        // Set up theme menu items
        darkThemeMenuItem.setOnAction(event -> setDarkTheme());
        lightThemeMenuItem.setOnAction(event -> setLightTheme());
        
        // Set up the messages scroll pane
        messagesScrollPane.vvalueProperty().bind(messagesContainer.heightProperty());
    }
    
    /**
     * Sets the message handler.
     * 
     * @param messageHandler The message handler
     */
    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        
        // Set up error listener
        messageHandler.setErrorListener(this::handleError);
        
        // Set up connection status listener
        messageHandler.setConnectionStatusListener(this::handleConnectionStatus);
    }
    
    /**
     * Sets the primary stage.
     * 
     * @param primaryStage The primary stage
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    
    /**
     * Initializes the controller after login.
     */
    public void initializeAfterLogin() {
        // Get the current user ID
        currentUserId = messageHandler.getCurrentUserId();
        
        // Load DM conversations
        loadDMConversations();
        
        // Load group conversations
        loadGroupConversations();
    }
    
    /**
     * Loads DM conversations.
     */
    private void loadDMConversations() {
        // In a real application, this would load conversations from the server
        // For now, we'll just create some dummy conversations
        
        // Clear existing conversations
        dmConversations.clear();
        
        // Load conversations from the server
        // This would be a real API call in a production application
    }
    
    /**
     * Loads group conversations.
     */
    private void loadGroupConversations() {
        // In a real application, this would load conversations from the server
        // For now, we'll just create some dummy conversations
        
        // Clear existing conversations
        groupConversations.clear();
        
        // Load conversations from the server
        // This would be a real API call in a production application
    }
    
    /**
     * Opens a DM conversation.
     * 
     * @param conversation The conversation to open
     */
    private void openDMConversation(DMConversation conversation) {
        // Switch to the DM tab if not already selected
        sidebarTabPane.getSelectionModel().select(dmTab);
        
        // Set the current chat ID
        currentChatId = conversation.getChatId();
        
        // Update the chat header
        String otherUserId = conversation.getOtherUserId(currentUserId);
        User otherUser = users.get(otherUserId);
        String displayName = otherUser != null ? otherUser.getDisplayName() : otherUserId;
        chatHeaderLabel.setText(displayName);
        
        // Clear the messages container
        messagesContainer.getChildren().clear();
        lastDateLabels.clear();
        
        // Mark the conversation as read
        conversation.markAsRead();
        
        // Connect to the chat
        messageHandler.connectToChat(currentChatId)
                .thenRun(() -> {
                    // Load messages
                    List<Message> messages = conversation.getMessages();
                    for (Message message : messages) {
                        addMessageToUI(message);
                    }
                })
                .exceptionally(ex -> {
                    handleError("Failed to connect to chat: " + ex.getMessage());
                    return null;
                });
        
        // Set up message listener
        setupMessageListener(currentChatId);
        
        // Set up user status listener
        setupUserStatusListener(currentChatId);
        
        // Update UI
        dmListView.refresh();
    }
    
    /**
     * Opens a group conversation.
     * 
     * @param conversation The conversation to open
     */
    private void openGroupConversation(GroupConversation conversation) {
        // Switch to the groups tab if not already selected
        sidebarTabPane.getSelectionModel().select(groupsTab);
        
        // Set the current chat ID
        currentChatId = conversation.getChatId();
        
        // Update the chat header
        Group group = conversation.getGroup();
        chatHeaderLabel.setText(group.getName() + " (" + group.getMemberCount() + " members)");
        
        // Clear the messages container
        messagesContainer.getChildren().clear();
        lastDateLabels.clear();
        
        // Mark the conversation as read
        conversation.markAsRead();
        
        // Connect to the chat
        messageHandler.connectToChat(currentChatId)
                .thenRun(() -> {
                    // Load messages
                    List<Message> messages = conversation.getMessages();
                    for (Message message : messages) {
                        addMessageToUI(message);
                    }
                })
                .exceptionally(ex -> {
                    handleError("Failed to connect to chat: " + ex.getMessage());
                    return null;
                });
        
        // Set up message listener
        setupMessageListener(currentChatId);
        
        // Set up user status listener
        setupUserStatusListener(currentChatId);
        
        // Update UI
        groupListView.refresh();
    }
    
    /**
     * Sets up a message listener for a chat.
     * 
     * @param chatId The chat ID
     */
    private void setupMessageListener(String chatId) {
        // Remove existing listener if any
        Consumer<Message> existingListener = messageListeners.get(chatId);
        if (existingListener != null) {
            messageHandler.removeMessageListener(chatId, existingListener);
        }
        
        // Create a new listener
        Consumer<Message> listener = this::addMessageToUI;
        messageListeners.put(chatId, listener);
        
        // Add the listener
        messageHandler.addMessageListener(chatId, listener);
    }
    
    /**
     * Sets up a user status listener for a chat.
     * 
     * @param chatId The chat ID
     */
    private void setupUserStatusListener(String chatId) {
        // Remove existing listener if any
        Consumer<User> existingListener = userStatusListeners.get(chatId);
        if (existingListener != null) {
            messageHandler.removeUserStatusListener(chatId, existingListener);
        }
        
        // Create a new listener
        Consumer<User> listener = this::handleUserStatusChange;
        userStatusListeners.put(chatId, listener);
        
        // Add the listener
        messageHandler.addUserStatusListener(chatId, listener);
    }
    
    /**
     * Adds a message to the UI.
     * 
     * @param message The message to add
     */
    private void addMessageToUI(Message message) {
        Platform.runLater(() -> {
            // Check if we need to add a date separator
            LocalDate messageDate = LocalDate.ofEpochDay(message.getTimestamp() / (24 * 60 * 60 * 1000));
            LocalDate lastDate = lastDateLabels.get(message.getChatId());
            
            if (lastDate == null || !lastDate.equals(messageDate)) {
                // Add a date separator
                Label dateLabel = new Label(messageDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
                dateLabel.getStyleClass().add("date-separator");
                dateLabel.setMaxWidth(Double.MAX_VALUE);
                dateLabel.setAlignment(Pos.CENTER);
                messagesContainer.getChildren().add(dateLabel);
                
                // Update the last date
                lastDateLabels.put(message.getChatId(), messageDate);
            }
            
            // Create the message bubble
            HBox messageBox = new HBox(10);
            messageBox.setPadding(new Insets(5, 10, 5, 10));
            
            boolean isCurrentUser = message.getSenderId().equals(currentUserId);
            
            if (message.isSystemMessage()) {
                // System message
                Label systemLabel = new Label(message.getContent());
                systemLabel.getStyleClass().add("system-message");
                systemLabel.setMaxWidth(Double.MAX_VALUE);
                systemLabel.setAlignment(Pos.CENTER);
                
                messagesContainer.getChildren().add(systemLabel);
            } else {
                // Regular message
                if (!isCurrentUser) {
                    // Add avatar for other users
                    Circle avatar = new Circle(20);
                    avatar.getStyleClass().add("avatar-circle");
                    
                    User user = users.get(message.getSenderId());
                    String initials = user != null ? user.getAvatarInitials() : 
                                     message.getSenderName().substring(0, Math.min(2, message.getSenderName().length()));
                    
                    Label initialsLabel = new Label(initials);
                    initialsLabel.getStyleClass().add("avatar-text");
                    
                    StackPane avatarPane = new StackPane(avatar, initialsLabel);
                    messageBox.getChildren().add(avatarPane);
                }
                
                // Create the message content
                VBox contentBox = new VBox(3);
                
                if (!isCurrentUser) {
                    // Add sender name for messages from others
                    Label nameLabel = new Label(message.getSenderName());
                    nameLabel.getStyleClass().add("sender-name");
                    contentBox.getChildren().add(nameLabel);
                }
                
                // Message bubble
                TextFlow textFlow = new TextFlow();
                Text text = new Text(message.getContent());
                textFlow.getChildren().add(text);
                textFlow.getStyleClass().add(isCurrentUser ? "message-bubble-sent" : "message-bubble-received");
                
                // Time label
                Label timeLabel = new Label(message.getFormattedTime());
                timeLabel.getStyleClass().add("time-label");
                
                HBox bubbleBox = new HBox();
                if (isCurrentUser) {
                    bubbleBox.getChildren().addAll(timeLabel, textFlow);
                    bubbleBox.setAlignment(Pos.CENTER_RIGHT);
                } else {
                    bubbleBox.getChildren().addAll(textFlow, timeLabel);
                }
                
                contentBox.getChildren().add(bubbleBox);
                messageBox.getChildren().add(contentBox);
                
                // Set alignment based on sender
                if (isCurrentUser) {
                    messageBox.setAlignment(Pos.CENTER_RIGHT);
                    HBox.setHgrow(contentBox, Priority.ALWAYS);
                } else {
                    messageBox.setAlignment(Pos.CENTER_LEFT);
                }
                
                messagesContainer.getChildren().add(messageBox);
            }
        });
    }
    
    /**
     * Sends a message.
     */
    private void sendMessage() {
        if (currentChatId == null) {
            return;
        }
        
        String content = messageInput.getText().trim();
        if (content.isEmpty()) {
            return;
        }
        
        // Clear the input
        messageInput.clear();
        
        // Send the message
        messageHandler.sendMessage(currentChatId, content)
                .exceptionally(ex -> {
                    handleError("Failed to send message: " + ex.getMessage());
                    return null;
                });
    }
    
    /**
     * Shows the emoji picker.
     * 
     * @param event The action event
     */
    private void showEmojiPicker(ActionEvent event) {
        try {
            // Load the emoji picker dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/emoji-picker.fxml"));
            Parent root = loader.load();
            
            // Get the controller
            EmojiPickerController controller = loader.getController();
            controller.setMessageInput(messageInput);
            
            // Create the dialog
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(primaryStage);
            dialog.setTitle("Emoji Picker");
            dialog.setScene(new Scene(root));
            
            // Show the dialog
            dialog.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load emoji picker", e);
            handleError("Failed to load emoji picker: " + e.getMessage());
        }
    }
    
    /**
     * Shows the new DM dialog.
     * 
     * @param event The action event
     */
    private void showNewDmDialog(ActionEvent event) {
        try {
            // Load the new DM dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/new-dm-dialog.fxml"));
            Parent root = loader.load();
            
            // Get the controller
            NewDMController controller = loader.getController();
            controller.setMessageHandler(messageHandler);
            controller.setMainController(this);
            
            // Create the dialog
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(primaryStage);
            dialog.setTitle("New Direct Message");
            dialog.setScene(new Scene(root));
            
            // Show the dialog
            dialog.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load new DM dialog", e);
            handleError("Failed to load new DM dialog: " + e.getMessage());
        }
    }
    
    /**
     * Shows the new group dialog.
     * 
     * @param event The action event
     */
    private void showNewGroupDialog(ActionEvent event) {
        try {
            // Load the new group dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/new-group-dialog.fxml"));
            Parent root = loader.load();
            
            // Get the controller
            NewGroupController controller = loader.getController();
            controller.setMessageHandler(messageHandler);
            controller.setMainController(this);
            
            // Create the dialog
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(primaryStage);
            dialog.setTitle("New Group");
            dialog.setScene(new Scene(root));
            
            // Show the dialog
            dialog.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load new group dialog", e);
            handleError("Failed to load new group dialog: " + e.getMessage());
        }
    }
    
    /**
     * Shows the search dialog.
     * 
     * @param event The action event
     */
    private void showSearchDialog(ActionEvent event) {
        try {
            // Load the search dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/search-dialog.fxml"));
            Parent root = loader.load();
            
            // Get the controller
            SearchController controller = loader.getController();
            controller.setMessageHandler(messageHandler);
            controller.setMainController(this);
            
            // Create the dialog
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(primaryStage);
            dialog.setTitle("Search");
            dialog.setScene(new Scene(root));
            
            // Show the dialog
            dialog.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load search dialog", e);
            handleError("Failed to load search dialog: " + e.getMessage());
        }
    }
    
    /**
     * Handles the logout button click.
     * 
     * @param event The action event
     */
    private void handleLogout(ActionEvent event) {
        // Logout
        messageHandler.logout();
        
        try {
            // Load the login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set the message handler
            LoginController controller = loader.getController();
            controller.setMessageHandler(messageHandler);
            controller.setPrimaryStage(primaryStage);
            
            // Set up the scene
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/modern-theme.css").toExternalForm());
            
            // Set up the stage
            primaryStage.setTitle("Espresso Chat - Login");
            primaryStage.setScene(scene);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load login screen", e);
            handleError("Failed to load login screen: " + e.getMessage());
        }
    }
    
    /**
     * Handles an error.
     * 
     * @param message The error message
     */
    private void handleError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Handles a connection status change.
     * 
     * @param connected Whether the connection is established
     */
    private void handleConnectionStatus(boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                // Connection established
                chatHeaderLabel.setTextFill(Color.BLACK);
            } else {
                // Connection lost
                chatHeaderLabel.setTextFill(Color.RED);
            }
        });
    }
    
    /**
     * Handles a user status change.
     * 
     * @param user The user whose status changed
     */
    private void handleUserStatusChange(User user) {
        Platform.runLater(() -> {
            // Update the user in the users map
            users.put(user.getUserId(), user);
            
            // Refresh the UI
            dmListView.refresh();
            groupListView.refresh();
        });
    }
    
    /**
     * Adds a DM conversation.
     * 
     * @param conversation The conversation to add
     */
    public void addDMConversation(DMConversation conversation) {
        Platform.runLater(() -> {
            // Check if the conversation already exists
            Optional<DMConversation> existingConversation = dmConversations.stream()
                    .filter(c -> c.getChatId().equals(conversation.getChatId()))
                    .findFirst();
            
            if (existingConversation.isPresent()) {
                // Conversation already exists, do nothing
                return;
            }
            
            // Add the conversation
            dmConversations.add(conversation);
            
            // Open the conversation
            dmListView.getSelectionModel().select(conversation);
        });
    }
    
    /**
     * Adds a group conversation.
     * 
     * @param conversation The conversation to add
     */
    public void addGroupConversation(GroupConversation conversation) {
        Platform.runLater(() -> {
            // Check if the conversation already exists
            Optional<GroupConversation> existingConversation = groupConversations.stream()
                    .filter(c -> c.getChatId().equals(conversation.getChatId()))
                    .findFirst();
            
            if (existingConversation.isPresent()) {
                // Conversation already exists, do nothing
                return;
            }
            
            // Add the conversation
            groupConversations.add(conversation);
            
            // Add the group to the groups map
            groups.put(conversation.getGroup().getGroupId(), conversation.getGroup());
            
            // Open the conversation
            groupListView.getSelectionModel().select(conversation);
        });
    }
    
    /**
     * Sets the dark theme.
     */
    private void setDarkTheme() {
        if (isDarkTheme) {
            return;
        }
        
        isDarkTheme = true;
        
        // Get the current scene
        Scene scene = rootPane.getScene();
        if (scene != null) {
            // Remove the light theme
            scene.getStylesheets().remove(getClass().getResource("/css/light-theme.css").toExternalForm());
            
            // Add the dark theme
            scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        }
    }
    
    /**
     * Sets the light theme.
     */
    private void setLightTheme() {
        if (!isDarkTheme) {
            return;
        }
        
        isDarkTheme = false;
        
        // Get the current scene
        Scene scene = rootPane.getScene();
        if (scene != null) {
            // Remove the dark theme
            scene.getStylesheets().remove(getClass().getResource("/css/dark-theme.css").toExternalForm());
            
            // Add the light theme
            scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());
        }
    }
    
    /**
     * Custom cell for DM conversations.
     */
    private class DMConversationCell extends ListCell<DMConversation> {
        private final HBox content;
        private final Circle avatar;
        private final Label initialsLabel;
        private final VBox textContainer;
        private final Label nameLabel;
        private final Label previewLabel;
        private final Circle unreadIndicator;
        
        public DMConversationCell() {
            // Create the avatar
            avatar = new Circle(20);
            avatar.getStyleClass().add("avatar-circle");
            
            initialsLabel = new Label();
            initialsLabel.getStyleClass().add("avatar-text");
            
            StackPane avatarPane = new StackPane(avatar, initialsLabel);
            
            // Create the text container
            nameLabel = new Label();
            nameLabel.getStyleClass().add("conversation-name");
            
            previewLabel = new Label();
            previewLabel.getStyleClass().add("conversation-preview");
            
            textContainer = new VBox(3, nameLabel, previewLabel);
            textContainer.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textContainer, Priority.ALWAYS);
            
            // Create the unread indicator
            unreadIndicator = new Circle(5);
            unreadIndicator.getStyleClass().add("unread-indicator");
            unreadIndicator.setVisible(false);
            
            // Create the content
            content = new HBox(10, avatarPane, textContainer, unreadIndicator);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(5, 10, 5, 10));
        }
        
        @Override
        protected void updateItem(DMConversation conversation, boolean empty) {
            super.updateItem(conversation, empty);
            
            if (empty || conversation == null) {
                setGraphic(null);
            } else {
                // Get the other user
                String otherUserId = conversation.getOtherUserId(currentUserId);
                User otherUser = users.get(otherUserId);
                String displayName = otherUser != null ? otherUser.getDisplayName() : otherUserId;
                
                // Set the name
                nameLabel.setText(displayName);
                
                // Set the initials
                String initials = otherUser != null ? otherUser.getAvatarInitials() : 
                                 displayName.substring(0, Math.min(2, displayName.length()));
                initialsLabel.setText(initials);
                
                // Set the preview
                Message lastMessage = conversation.getLastMessage();
                if (lastMessage != null) {
                    previewLabel.setText(lastMessage.getContent());
                } else {
                    previewLabel.setText("No messages yet");
                }
                
                // Set the unread indicator
                unreadIndicator.setVisible(conversation.isUnread());
                
                // Set the graphic
                setGraphic(content);
            }
        }
    }
    
    /**
     * Custom cell for group conversations.
     */
    private class GroupConversationCell extends ListCell<GroupConversation> {
        private final HBox content;
        private final Circle avatar;
        private final Label initialsLabel;
        private final VBox textContainer;
        private final Label nameLabel;
        private final Label previewLabel;
        private final Circle unreadIndicator;
        
        public GroupConversationCell() {
            // Create the avatar
            avatar = new Circle(20);
            avatar.getStyleClass().add("avatar-circle");
            avatar.getStyleClass().add("group-avatar");
            
            initialsLabel = new Label();
            initialsLabel.getStyleClass().add("avatar-text");
            
            StackPane avatarPane = new StackPane(avatar, initialsLabel);
            
            // Create the text container
            nameLabel = new Label();
            nameLabel.getStyleClass().add("conversation-name");
            
            previewLabel = new Label();
            previewLabel.getStyleClass().add("conversation-preview");
            
            textContainer = new VBox(3, nameLabel, previewLabel);
            textContainer.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textContainer, Priority.ALWAYS);
            
            // Create the unread indicator
            unreadIndicator = new Circle(5);
            unreadIndicator.getStyleClass().add("unread-indicator");
            unreadIndicator.setVisible(false);
            
            // Create the content
            content = new HBox(10, avatarPane, textContainer, unreadIndicator);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(5, 10, 5, 10));
        }
        
        @Override
        protected void updateItem(GroupConversation conversation, boolean empty) {
            super.updateItem(conversation, empty);
            
            if (empty || conversation == null) {
                setGraphic(null);
            } else {
                // Get the group
                Group group = conversation.getGroup();
                
                // Set the name
                nameLabel.setText(group.getName());
                
                // Set the initials
                String initials = group.getName().substring(0, Math.min(2, group.getName().length())).toUpperCase();
                initialsLabel.setText(initials);
                
                // Set the preview
                Message lastMessage = conversation.getLastMessage();
                if (lastMessage != null) {
                    previewLabel.setText(lastMessage.getSenderName() + ": " + lastMessage.getContent());
                } else {
                    previewLabel.setText("No messages yet");
                }
                
                // Set the unread indicator
                unreadIndicator.setVisible(conversation.isUnread());
                
                // Set the graphic
                setGraphic(content);
            }
        }
    }
}