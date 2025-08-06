package com.espresso.client.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for the emoji picker dialog.
 */
public class EmojiPickerController implements Initializable {
    @FXML private TabPane emojiTabPane;
    @FXML private Tab smileyTab;
    @FXML private Tab objectsTab;
    @FXML private Tab animalsTab;
    @FXML private Tab foodTab;
    @FXML private Tab travelTab;
    @FXML private Tab symbolsTab;
    @FXML private Tab recentTab;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private VBox searchResultsBox;
    @FXML private ScrollPane searchScrollPane;
    
    private TextArea messageInput;
    private final Map<String, List<String>> emojiCategories = new HashMap<>();
    private final List<String> recentEmojis = new java.util.ArrayList<>();
    private static final int MAX_RECENT_EMOJIS = 20;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize emoji categories
        initializeEmojiCategories();
        
        // Set up the tabs
        setupEmojiTab(smileyTab, emojiCategories.get("smileys"));
        setupEmojiTab(objectsTab, emojiCategories.get("objects"));
        setupEmojiTab(animalsTab, emojiCategories.get("animals"));
        setupEmojiTab(foodTab, emojiCategories.get("food"));
        setupEmojiTab(travelTab, emojiCategories.get("travel"));
        setupEmojiTab(symbolsTab, emojiCategories.get("symbols"));
        setupEmojiTab(recentTab, recentEmojis);
        
        // Set up the search button
        searchButton.setOnAction(event -> performSearch());
        
        // Set up the search field
        searchField.setOnAction(event -> performSearch());
    }
    
    /**
     * Sets the message input.
     * 
     * @param messageInput The message input
     */
    public void setMessageInput(TextArea messageInput) {
        this.messageInput = messageInput;
    }
    
    /**
     * Initializes the emoji categories.
     */
    private void initializeEmojiCategories() {
        // Smileys and people
        emojiCategories.put("smileys", Arrays.asList(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
            "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
            "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
            "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣", "😖",
            "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬", "🤯"
        ));
        
        // Objects
        emojiCategories.put("objects", Arrays.asList(
            "⌚", "📱", "💻", "⌨️", "🖥️", "🖨️", "🖱️", "🖲️", "🕹️", "🗜️",
            "💽", "💾", "💿", "📀", "📼", "📷", "📸", "📹", "🎥", "📽️",
            "🎞️", "📞", "☎️", "📟", "📠", "📺", "📻", "🎙️", "🎚️", "🎛️",
            "🧭", "⏱️", "⏲️", "⏰", "🕰️", "⌛", "⏳", "📡", "🔋", "🔌"
        ));
        
        // Animals and nature
        emojiCategories.put("animals", Arrays.asList(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯",
            "🦁", "🐮", "🐷", "🐽", "🐸", "🐵", "🙈", "🙉", "🙊", "🐒",
            "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅", "🦉", "🦇",
            "🐺", "🐗", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞", "🐜"
        ));
        
        // Food and drink
        emojiCategories.put("food", Arrays.asList(
            "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍈",
            "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦",
            "🥬", "🥒", "🌶️", "🌽", "🥕", "🧄", "🧅", "🥔", "🍠", "🥐",
            "🥯", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳", "🧈", "🥞", "🧇"
        ));
        
        // Travel and places
        emojiCategories.put("travel", Arrays.asList(
            "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐",
            "🚚", "🚛", "🚜", "🦯", "🦽", "🦼", "🛴", "🚲", "🛵", "🏍️",
            "🛺", "🚨", "🚔", "🚍", "🚘", "🚖", "🚡", "🚠", "🚟", "🚃",
            "🚋", "🚞", "🚝", "🚄", "🚅", "🚈", "🚂", "🚆", "🚇", "🚊"
        ));
        
        // Symbols
        emojiCategories.put("symbols", Arrays.asList(
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
            "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "☮️",
            "✝️", "☪️", "🕉️", "☸️", "✡️", "🔯", "🕎", "☯️", "☦️", "🛐",
            "⛎", "♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐"
        ));
    }
    
    /**
     * Sets up an emoji tab.
     * 
     * @param tab The tab
     * @param emojis The list of emojis
     */
    private void setupEmojiTab(Tab tab, List<String> emojis) {
        FlowPane flowPane = new FlowPane();
        flowPane.setPadding(new Insets(10));
        flowPane.setHgap(10);
        flowPane.setVgap(10);
        
        for (String emoji : emojis) {
            Button button = new Button(emoji);
            button.getStyleClass().add("emoji-button");
            button.setOnAction(event -> insertEmoji(emoji));
            flowPane.getChildren().add(button);
        }
        
        ScrollPane scrollPane = new ScrollPane(flowPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        tab.setContent(scrollPane);
    }
    
    /**
     * Performs a search for emojis.
     */
    private void performSearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            searchResultsBox.getChildren().clear();
            searchScrollPane.setVisible(false);
            return;
        }
        
        // Search for emojis
        List<String> results = emojiCategories.values().stream()
                .flatMap(List::stream)
                .filter(emoji -> {
                    // In a real application, this would search by emoji name or description
                    // For now, we'll just return all emojis
                    return true;
                })
                .collect(Collectors.toList());
        
        // Display results
        searchResultsBox.getChildren().clear();
        
        FlowPane flowPane = new FlowPane();
        flowPane.setPadding(new Insets(10));
        flowPane.setHgap(10);
        flowPane.setVgap(10);
        
        for (String emoji : results) {
            Button button = new Button(emoji);
            button.getStyleClass().add("emoji-button");
            button.setOnAction(event -> insertEmoji(emoji));
            flowPane.getChildren().add(button);
        }
        
        searchResultsBox.getChildren().add(flowPane);
        searchScrollPane.setVisible(true);
    }
    
    /**
     * Inserts an emoji into the message input.
     * 
     * @param emoji The emoji to insert
     */
    private void insertEmoji(String emoji) {
        if (messageInput != null) {
            messageInput.insertText(messageInput.getCaretPosition(), emoji);
            
            // Add to recent emojis
            if (recentEmojis.contains(emoji)) {
                recentEmojis.remove(emoji);
            }
            recentEmojis.add(0, emoji);
            
            // Limit the number of recent emojis
            if (recentEmojis.size() > MAX_RECENT_EMOJIS) {
                recentEmojis.remove(recentEmojis.size() - 1);
            }
            
            // Update the recent tab
            setupEmojiTab(recentTab, recentEmojis);
            
            // Close the dialog
            Stage stage = (Stage) emojiTabPane.getScene().getWindow();
            stage.close();
        }
    }
}