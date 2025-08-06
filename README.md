# Espresso Chat

A real-time chat application with a JavaFX client and Cloudflare Workers backend.

## Project Structure

- `src/main/java/com/espresso/client/` - Java client application
- `worker.js`, `dm-room.js`, `group-room.js` - Cloudflare Workers backend
- `wrangler.toml` - Cloudflare Workers configuration

## Java Client

### Requirements

- Java 11 or higher
- Maven

### Compilation

To compile the Java client application, run:

```bash
mvn clean package
```

This will create an executable JAR file in the `target` directory.

### Running the Application

To run the application using Maven:

```bash
mvn javafx:run
```

Or to run the JAR file directly:

```bash
java -jar target/espresso-chat-1.0-SNAPSHOT.jar
```

## Cloudflare Workers Backend

### Requirements

- Node.js 14 or higher
- Wrangler CLI (`npm install -g wrangler`)
- Cloudflare account

### Configuration

Before deploying the Workers backend, you need to:

1. Create a KV namespace in your Cloudflare account
2. Update the `wrangler.toml` file with your KV namespace ID
3. (Optional) Update the JWT secret in `worker.js`

### Deployment

To deploy the Workers backend:

1. Login to your Cloudflare account:

```bash
wrangler login
```

2. Deploy the Workers:

```bash
wrangler publish
```

### Local Development

To run the Workers locally:

```bash
wrangler dev
```

## Features

- User registration and authentication
- Direct messaging between users
- Group chat functionality
- Real-time message delivery via WebSockets
- Modern UI with dark/light theme support
- Emoji picker
- User status indicators
- Message history

## Implementation Details

### Java Client

The Java client is built using JavaFX for the UI and uses the Java 11+ WebSocket API for real-time communication. It follows the MVC pattern with:

- Models: User, Message, Group, DMConversation, GroupConversation
- Controllers: LoginController, MainChatController, etc.
- Views: FXML files for UI layout

### Cloudflare Workers Backend

The backend uses Cloudflare Workers with:

- Durable Objects for stateful WebSocket connections
- KV storage for user data, group data, and message persistence
- JWT authentication for secure API access

## WebSocket Protocol

### Outgoing Messages (Client to Server)

```json
{
  "action": "send_message|join_chat|leave_chat",
  "chatId": "dm_alice_bob or group_general",
  "content": "message text with emoji support 😀",
  "timestamp": 1234567890,
  "messageType": "text"
}
```

### Incoming Messages (Server to Client)

```json
{
  "type": "message|user_joined|user_left|error",
  "senderId": "alice123", 
  "content": "message content",
  "timestamp": 1234567890,
  "chatId": "current_chat_id"
}
```

## Authentication Flow

1. Register: HTTP POST to `/api/register` with `{userId, password, displayName}`
2. Login: HTTP POST to `/api/login` returns `{token, userId, success}`
3. WebSocket connection with `Authorization: Bearer {token}` header

## License

MIT