package com.espresso.client;

import com.espresso.client.models.Message;
import com.espresso.client.models.User;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

/**
 * Handles all network communication for the chat application.
 */
public class MessageHandler {
    private static final Logger LOGGER = Logger.getLogger(MessageHandler.class.getName());
    private static final String API_BASE_URL = "https://chat-app.workers.dev";
    private static final String WS_BASE_URL = "wss://chat-app.workers.dev";
    
    private final HttpClient httpClient;
    private final ExecutorService executorService;
    private final Map<String, WebSocket> chatSockets;
    private final Map<String, List<Consumer<Message>>> messageListeners;
    private final Map<String, List<Consumer<User>>> userStatusListeners;
    
    private String authToken;
    private String currentUserId;
    private Consumer<String> errorListener;
    private Consumer<Boolean> connectionStatusListener;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_MS = 2000;

    /**
     * Creates a new MessageHandler instance.
     */
    public MessageHandler() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.executorService = Executors.newCachedThreadPool();
        this.chatSockets = new ConcurrentHashMap<>();
        this.messageListeners = new ConcurrentHashMap<>();
        this.userStatusListeners = new ConcurrentHashMap<>();
    }

    /**
     * Sets the error listener.
     * 
     * @param listener The error listener
     */
    public void setErrorListener(Consumer<String> listener) {
        this.errorListener = listener;
    }

    /**
     * Sets the connection status listener.
     * 
     * @param listener The connection status listener
     */
    public void setConnectionStatusListener(Consumer<Boolean> listener) {
        this.connectionStatusListener = listener;
    }

    /**
     * Registers a new user.
     * 
     * @param userId The user ID
     * @param password The password
     * @param displayName The display name
     * @return A CompletableFuture that completes with true if registration was successful, false otherwise
     */
    public CompletableFuture<Boolean> register(String userId, String password, String displayName) {
        JsonObject json = Json.createObjectBuilder()
                .add("userId", userId)
                .add("password", password)
                .add("displayName", displayName)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/api/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    if (statusCode == 201) {
                        return true;
                    } else {
                        String errorMsg = "Registration failed with status code: " + statusCode;
                        if (errorListener != null) {
                            errorListener.accept(errorMsg);
                        }
                        LOGGER.warning(errorMsg);
                        return false;
                    }
                }, executorService)
                .exceptionally(ex -> {
                    String errorMsg = "Registration failed: " + ex.getMessage();
                    if (errorListener != null) {
                        errorListener.accept(errorMsg);
                    }
                    LOGGER.log(Level.SEVERE, errorMsg, ex);
                    return false;
                });
    }

    /**
     * Logs in a user.
     * 
     * @param userId The user ID
     * @param password The password
     * @return A CompletableFuture that completes with true if login was successful, false otherwise
     */
    public CompletableFuture<Boolean> login(String userId, String password) {
        JsonObject json = Json.createObjectBuilder()
                .add("userId", userId)
                .add("password", password)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/api/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    if (statusCode == 200) {
                        try (JsonReader reader = Json.createReader(new java.io.StringReader(response.body()))) {
                            JsonObject responseJson = reader.readObject();
                            if (responseJson.getBoolean("success", false)) {
                                this.authToken = responseJson.getString("token");
                                this.currentUserId = userId;
                                return true;
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Failed to parse login response", e);
                        }
                    }
                    
                    String errorMsg = "Login failed with status code: " + statusCode;
                    if (errorListener != null) {
                        errorListener.accept(errorMsg);
                    }
                    LOGGER.warning(errorMsg);
                    return false;
                }, executorService)
                .exceptionally(ex -> {
                    String errorMsg = "Login failed: " + ex.getMessage();
                    if (errorListener != null) {
                        errorListener.accept(errorMsg);
                    }
                    LOGGER.log(Level.SEVERE, errorMsg, ex);
                    return false;
                });
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        // Close all WebSocket connections
        for (WebSocket socket : chatSockets.values()) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "User logged out");
        }
        
        chatSockets.clear();
        authToken = null;
        currentUserId = null;
        
        if (connectionStatusListener != null) {
            connectionStatusListener.accept(false);
        }
    }

    /**
     * Gets the current user ID.
     * 
     * @return The current user ID
     */
    public String getCurrentUserId() {
        return currentUserId;
    }

    /**
     * Checks if a user is authenticated.
     * 
     * @return true if a user is authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return authToken != null && currentUserId != null;
    }

    /**
     * Connects to a chat.
     * 
     * @param chatId The chat ID
     * @return A CompletableFuture that completes when the connection is established
     */
    public CompletableFuture<Void> connectToChat(String chatId) {
        if (!isAuthenticated()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Not authenticated"));
            return future;
        }

        if (chatSockets.containsKey(chatId)) {
            // Already connected to this chat
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> connectionFuture = new CompletableFuture<>();
        
        WebSocket.Listener listener = new WebSocket.Listener() {
            StringBuilder messageBuffer = new StringBuilder();
            
            @Override
            public void onOpen(WebSocket webSocket) {
                LOGGER.info("WebSocket connection opened for chat: " + chatId);
                chatSockets.put(chatId, webSocket);
                
                if (connectionStatusListener != null) {
                    connectionStatusListener.accept(true);
                }
                
                connectionFuture.complete(null);
                
                // Request more messages
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                messageBuffer.append(data);
                
                if (last) {
                    String fullMessage = messageBuffer.toString();
                    messageBuffer = new StringBuilder();
                    
                    try (JsonReader reader = Json.createReader(new java.io.StringReader(fullMessage))) {
                        JsonObject json = reader.readObject();
                        handleIncomingMessage(json, chatId);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Failed to parse WebSocket message: " + fullMessage, e);
                        if (errorListener != null) {
                            errorListener.accept("Failed to parse message: " + e.getMessage());
                        }
                    }
                }
                
                // Request more messages
                webSocket.request(1);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                LOGGER.info("WebSocket connection closed for chat " + chatId + ": " + statusCode + " - " + reason);
                chatSockets.remove(chatId);
                
                if (statusCode != WebSocket.NORMAL_CLOSURE && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    // Attempt to reconnect
                    reconnectAttempts++;
                    LOGGER.info("Attempting to reconnect to chat " + chatId + " (attempt " + reconnectAttempts + ")");
                    
                    executorService.submit(() -> {
                        try {
                            Thread.sleep(RECONNECT_DELAY_MS);
                            connectToChat(chatId);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                } else {
                    reconnectAttempts = 0;
                    if (connectionStatusListener != null) {
                        connectionStatusListener.accept(false);
                    }
                }
                
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                LOGGER.log(Level.SEVERE, "WebSocket error for chat " + chatId, error);
                if (errorListener != null) {
                    errorListener.accept("WebSocket error: " + error.getMessage());
                }
                
                chatSockets.remove(chatId);
                
                if (connectionStatusListener != null) {
                    connectionStatusListener.accept(false);
                }
            }
        };

        String wsUrl = WS_BASE_URL + "/ws?chatId=" + chatId;
        
        WebSocket.Builder wsBuilder = httpClient.newWebSocketBuilder();
        wsBuilder.header("Authorization", "Bearer " + authToken);
        
        wsBuilder.buildAsync(URI.create(wsUrl), listener)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Failed to connect to WebSocket for chat " + chatId, ex);
                    if (errorListener != null) {
                        errorListener.accept("Failed to connect: " + ex.getMessage());
                    }
                    connectionFuture.completeExceptionally(ex);
                    return null;
                });
        
        return connectionFuture;
    }

    /**
     * Disconnects from a chat.
     * 
     * @param chatId The chat ID
     */
    public void disconnectFromChat(String chatId) {
        WebSocket socket = chatSockets.get(chatId);
        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "User left chat");
            chatSockets.remove(chatId);
        }
    }

    /**
     * Sends a message to a chat.
     * 
     * @param chatId The chat ID
     * @param content The message content
     * @return A CompletableFuture that completes when the message is sent
     */
    public CompletableFuture<Void> sendMessage(String chatId, String content) {
        if (!isAuthenticated()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Not authenticated"));
            return future;
        }

        WebSocket socket = chatSockets.get(chatId);
        if (socket == null) {
            // Not connected to this chat, try to connect first
            return connectToChat(chatId).thenCompose(v -> sendMessage(chatId, content));
        }

        // Validate message content
        if (content == null || content.trim().isEmpty()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Message content cannot be empty"));
            return future;
        }

        final String finalContent = content.trim().length() > 2000 ? 
            content.trim().substring(0, 2000) : content.trim();

        JsonObject json = Json.createObjectBuilder()
                .add("action", "send_message")
                .add("chatId", chatId)
                .add("content", finalContent)
                .add("timestamp", System.currentTimeMillis())
                .add("messageType", "text")
                .build();

        CompletableFuture<Void> sendFuture = new CompletableFuture<>();
        
        socket.sendText(json.toString(), true)
              .whenComplete((ws, ex) -> {
                  if (ex != null) {
                      LOGGER.log(Level.SEVERE, "Failed to send message to chat " + chatId, ex);
                      if (errorListener != null) {
                          errorListener.accept("Failed to send message: " + ex.getMessage());
                      }
                      sendFuture.completeExceptionally(ex);
                  } else {
                      sendFuture.complete(null);
                  }
              });
        
        return sendFuture;
    }

    /**
     * Joins a group chat.
     * 
     * @param groupId The group ID
     * @return A CompletableFuture that completes when the group is joined
     */
    public CompletableFuture<Void> joinGroupChat(String groupId) {
        if (!isAuthenticated()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Not authenticated"));
            return future;
        }

        String chatId = "group_" + groupId;
        
        // First connect to the chat
        return connectToChat(chatId).thenCompose(v -> {
            WebSocket socket = chatSockets.get(chatId);
            if (socket == null) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new IllegalStateException("Failed to connect to group chat"));
                return future;
            }

            JsonObject json = Json.createObjectBuilder()
                    .add("action", "join_chat")
                    .add("chatId", chatId)
                    .add("timestamp", System.currentTimeMillis())
                    .build();

            CompletableFuture<Void> joinFuture = new CompletableFuture<>();
            
            socket.sendText(json.toString(), true)
                  .whenComplete((ws, ex) -> {
                      if (ex != null) {
                          LOGGER.log(Level.SEVERE, "Failed to join group chat " + groupId, ex);
                          if (errorListener != null) {
                              errorListener.accept("Failed to join group: " + ex.getMessage());
                          }
                          joinFuture.completeExceptionally(ex);
                      } else {
                          joinFuture.complete(null);
                      }
                  });
            
            return joinFuture;
        });
    }

    /**
     * Leaves a group chat.
     * 
     * @param groupId The group ID
     * @return A CompletableFuture that completes when the group is left
     */
    public CompletableFuture<Void> leaveGroupChat(String groupId) {
        if (!isAuthenticated()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Not authenticated"));
            return future;
        }

        String chatId = "group_" + groupId;
        WebSocket socket = chatSockets.get(chatId);
        if (socket == null) {
            // Not connected to this chat
            return CompletableFuture.completedFuture(null);
        }

        JsonObject json = Json.createObjectBuilder()
                .add("action", "leave_chat")
                .add("chatId", chatId)
                .add("timestamp", System.currentTimeMillis())
                .build();

        CompletableFuture<Void> leaveFuture = new CompletableFuture<>();
        
        socket.sendText(json.toString(), true)
              .whenComplete((ws, ex) -> {
                  if (ex != null) {
                      LOGGER.log(Level.SEVERE, "Failed to leave group chat " + groupId, ex);
                      if (errorListener != null) {
                          errorListener.accept("Failed to leave group: " + ex.getMessage());
                      }
                      leaveFuture.completeExceptionally(ex);
                  } else {
                      // Disconnect from the chat
                      disconnectFromChat(chatId);
                      leaveFuture.complete(null);
                  }
              });
        
        return leaveFuture;
    }

    /**
     * Creates a new group.
     * 
     * @param name The group name
     * @param description The group description
     * @return A CompletableFuture that completes with the group ID if creation was successful
     */
    public CompletableFuture<String> createGroup(String name, String description) {
        if (!isAuthenticated()) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Not authenticated"));
            return future;
        }

        JsonObject json = Json.createObjectBuilder()
                .add("name", name)
                .add("description", description)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/api/groups"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    if (statusCode == 201) {
                        try (JsonReader reader = Json.createReader(new java.io.StringReader(response.body()))) {
                            JsonObject responseJson = reader.readObject();
                            return responseJson.getString("groupId");
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Failed to parse create group response", e);
                            throw new RuntimeException("Failed to parse response", e);
                        }
                    } else {
                        String errorMsg = "Create group failed with status code: " + statusCode;
                        if (errorListener != null) {
                            errorListener.accept(errorMsg);
                        }
                        LOGGER.warning(errorMsg);
                        throw new RuntimeException(errorMsg);
                    }
                }, executorService)
                .exceptionally(ex -> {
                    String errorMsg = "Create group failed: " + ex.getMessage();
                    if (errorListener != null) {
                        errorListener.accept(errorMsg);
                    }
                    LOGGER.log(Level.SEVERE, errorMsg, ex);
                    throw new CompletionException(ex);
                });
    }

    /**
     * Searches for users.
     * 
     * @param query The search query
     * @return A CompletableFuture that completes with a list of users matching the query
     */
    public CompletableFuture<List<User>> searchUsers(String query) {
        if (!isAuthenticated()) {
            CompletableFuture<List<User>> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Not authenticated"));
            return future;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/api/users/search?q=" + query))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    if (statusCode == 200) {
                        List<User> users = new ArrayList<>();
                        try (JsonReader reader = Json.createReader(new java.io.StringReader(response.body()))) {
                            JsonValue jsonValue = reader.readValue();
                            if (jsonValue.getValueType() == JsonValue.ValueType.ARRAY) {
                                for (JsonValue item : jsonValue.asJsonArray()) {
                                    if (item.getValueType() == JsonValue.ValueType.OBJECT) {
                                        JsonObject userJson = item.asJsonObject();
                                        String userId = userJson.getString("userId");
                                        String displayName = userJson.getString("displayName");
                                        User user = new User(userId, displayName);
                                        user.setOnline(userJson.getBoolean("online", false));
                                        users.add(user);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Failed to parse search users response", e);
                        }
                        return users;
                    } else {
                        String errorMsg = "Search users failed with status code: " + statusCode;
                        if (errorListener != null) {
                            errorListener.accept(errorMsg);
                        }
                        LOGGER.warning(errorMsg);
                        return new ArrayList<User>();
                    }
                }, executorService)
                .exceptionally(ex -> {
                    String errorMsg = "Search users failed: " + ex.getMessage();
                    if (errorListener != null) {
                        errorListener.accept(errorMsg);
                    }
                    LOGGER.log(Level.SEVERE, errorMsg, ex);
                    return new ArrayList<User>();
                });
    }

    /**
     * Searches for groups.
     * 
     * @param query The search query
     * @return A CompletableFuture that completes with a list of groups matching the query
     */
    public CompletableFuture<List<Map<String, String>>> searchGroups(String query) {
        if (!isAuthenticated()) {
            CompletableFuture<List<Map<String, String>>> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Not authenticated"));
            return future;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/api/groups/search?q=" + query))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    if (statusCode == 200) {
                        List<Map<String, String>> groups = new ArrayList<>();
                        try (JsonReader reader = Json.createReader(new java.io.StringReader(response.body()))) {
                            JsonValue jsonValue = reader.readValue();
                            if (jsonValue.getValueType() == JsonValue.ValueType.ARRAY) {
                                for (JsonValue item : jsonValue.asJsonArray()) {
                                    if (item.getValueType() == JsonValue.ValueType.OBJECT) {
                                        JsonObject groupJson = item.asJsonObject();
                                        Map<String, String> group = new HashMap<>();
                                        group.put("groupId", groupJson.getString("groupId"));
                                        group.put("name", groupJson.getString("name"));
                                        group.put("description", groupJson.getString("description", ""));
                                        group.put("memberCount", String.valueOf(groupJson.getInt("memberCount", 0)));
                                        groups.add(group);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Failed to parse search groups response", e);
                        }
                        return groups;
                    } else {
                        String errorMsg = "Search groups failed with status code: " + statusCode;
                        if (errorListener != null) {
                            errorListener.accept(errorMsg);
                        }
                        LOGGER.warning(errorMsg);
                        return new ArrayList<Map<String, String>>();
                    }
                }, executorService)
                .exceptionally(ex -> {
                    String errorMsg = "Search groups failed: " + ex.getMessage();
                    if (errorListener != null) {
                        errorListener.accept(errorMsg);
                    }
                    LOGGER.log(Level.SEVERE, errorMsg, ex);
                    return new ArrayList<Map<String, String>>();
                });
    }

    /**
     * Adds a message listener for a specific chat.
     * 
     * @param chatId The chat ID
     * @param listener The message listener
     */
    public void addMessageListener(String chatId, Consumer<Message> listener) {
        messageListeners.computeIfAbsent(chatId, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Removes a message listener for a specific chat.
     * 
     * @param chatId The chat ID
     * @param listener The message listener to remove
     */
    public void removeMessageListener(String chatId, Consumer<Message> listener) {
        List<Consumer<Message>> listeners = messageListeners.get(chatId);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Adds a user status listener for a specific chat.
     * 
     * @param chatId The chat ID
     * @param listener The user status listener
     */
    public void addUserStatusListener(String chatId, Consumer<User> listener) {
        userStatusListeners.computeIfAbsent(chatId, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Removes a user status listener for a specific chat.
     * 
     * @param chatId The chat ID
     * @param listener The user status listener to remove
     */
    public void removeUserStatusListener(String chatId, Consumer<User> listener) {
        List<Consumer<User>> listeners = userStatusListeners.get(chatId);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Handles an incoming message from a WebSocket.
     * 
     * @param json The JSON message
     * @param chatId The chat ID
     */
    private void handleIncomingMessage(JsonObject json, String chatId) {
        String type = json.getString("type", "");
        
        switch (type) {
            case "message":
                handleChatMessage(json, chatId);
                break;
            case "user_joined":
            case "user_left":
                handleUserStatusChange(json, chatId, type.equals("user_joined"));
                break;
            case "error":
                handleErrorMessage(json);
                break;
            default:
                LOGGER.warning("Unknown message type: " + type);
        }
    }

    /**
     * Handles a chat message.
     * 
     * @param json The JSON message
     * @param chatId The chat ID
     */
    private void handleChatMessage(JsonObject json, String chatId) {
        String senderId = json.getString("senderId", "");
        String content = json.getString("content", "");
        long timestamp = json.getJsonNumber("timestamp").longValue();
        String messageId = json.getString("messageId", "msg_" + System.currentTimeMillis());
        String senderName = json.getString("senderName", senderId);
        
        Message message = new Message(
            messageId,
            senderId,
            senderName,
            content,
            timestamp,
            chatId,
            Message.MessageType.TEXT
        );
        
        List<Consumer<Message>> listeners = messageListeners.get(chatId);
        if (listeners != null) {
            for (Consumer<Message> listener : listeners) {
                listener.accept(message);
            }
        }
    }

    /**
     * Handles a user status change message.
     * 
     * @param json The JSON message
     * @param chatId The chat ID
     * @param joined Whether the user joined (true) or left (false)
     */
    private void handleUserStatusChange(JsonObject json, String chatId, boolean joined) {
        String userId = json.getString("senderId", "");
        String userName = json.getString("senderName", userId);
        
        User user = new User(userId, userName);
        user.setOnline(joined);
        
        // Notify user status listeners
        List<Consumer<User>> statusListeners = userStatusListeners.get(chatId);
        if (statusListeners != null) {
            for (Consumer<User> listener : statusListeners) {
                listener.accept(user);
            }
        }
        
        // Create a system message
        Message message;
        if (joined) {
            message = Message.createUserJoinedMessage(userId, userName, chatId);
        } else {
            message = Message.createUserLeftMessage(userId, userName, chatId);
        }
        
        // Notify message listeners
        List<Consumer<Message>> msgListeners = messageListeners.get(chatId);
        if (msgListeners != null) {
            for (Consumer<Message> listener : msgListeners) {
                listener.accept(message);
            }
        }
    }

    /**
     * Handles an error message.
     * 
     * @param json The JSON message
     */
    private void handleErrorMessage(JsonObject json) {
        String content = json.getString("content", "Unknown error");
        String chatId = json.getString("chatId", "");
        
        LOGGER.warning("Error message received: " + content);
        
        if (errorListener != null) {
            errorListener.accept(content);
        }
        
        // Create an error message
        Message errorMessage = Message.createErrorMessage(content, chatId);
        
        // Notify message listeners
        List<Consumer<Message>> listeners = messageListeners.get(chatId);
        if (listeners != null) {
            for (Consumer<Message> listener : listeners) {
                listener.accept(errorMessage);
            }
        }
    }

    /**
     * Shuts down the message handler.
     */
    public void shutdown() {
        // Close all WebSocket connections
        for (WebSocket socket : chatSockets.values()) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "Application shutting down");
        }
        
        chatSockets.clear();
        executorService.shutdown();
    }
}