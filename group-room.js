/**
 * Durable Object for handling group chat rooms.
 */

export class GroupRoom {
  constructor(state, env) {
    this.state = state;
    this.env = env;
    this.sessions = new Map();
    this.messageHistory = [];
    this.members = new Set();
    this.userStatus = new Map();
    this.MAX_HISTORY = 100; // Maximum number of messages to store
  }

  /**
   * Handles incoming requests to the Durable Object.
   * @param {Request} request - The incoming request
   * @param {Object} env - Environment variables and bindings
   * @returns {Response} The response
   */
  async fetch(request, env) {
    try {
      // Handle WebSocket upgrade
      if (request.headers.get('Upgrade') === 'websocket') {
        return this.handleWebSocketUpgrade(request);
      }

      // Handle HTTP requests
      return new Response('This endpoint only supports WebSocket connections', { status: 400 });
    } catch (error) {
      console.error('Error in GroupRoom fetch:', error);
      return new Response('Internal server error', { status: 500 });
    }
  }

  /**
   * Handles WebSocket upgrade requests.
   * @param {Request} request - The incoming request
   * @returns {Response} The WebSocket response
   */
  async handleWebSocketUpgrade(request) {
    // Extract user ID from request headers
    const userId = request.headers.get('X-User-ID');
    if (!userId) {
      // Try to get userId from request body as fallback
      try {
        const data = await request.json();
        if (data && data.userId) {
          return this.setupWebSocket(request, data.userId);
        }
      } catch (error) {
        console.error('Error parsing request body:', error);
      }
      return new Response('Missing userId in request headers or body', { status: 400 });
    }
    
    return this.setupWebSocket(request, userId);
  }
  
  /**
   * Sets up the WebSocket connection.
   * @param {Request} request - The incoming request
   * @param {string} userId - The user ID
   * @returns {Response} The WebSocket response
   */
  async setupWebSocket(request, userId) {

    // Get the group ID from the Durable Object ID
    const groupId = this.state.id.name.replace('group_', '');

    // Check if the user is a member of the group
    const groupJson = await this.env.CHAT_KV.get(`group:${groupId}`);
    if (!groupJson) {
      return new Response('Group not found', { status: 404 });
    }

    const group = JSON.parse(groupJson);
    if (!group.members.includes(userId)) {
      return new Response('User is not a member of this group', { status: 403 });
    }

    // Initialize members set if not already done
    if (this.members.size === 0) {
      for (const memberId of group.members) {
        this.members.add(memberId);
      }
    }

    // Accept the WebSocket connection
    const pair = new WebSocketPair();
    const [client, server] = Object.values(pair);

    // Set up the server-side WebSocket
    server.accept();

    // Store the session
    const sessionId = crypto.randomUUID();
    this.sessions.set(sessionId, {
      userId,
      webSocket: server,
      lastActive: Date.now(),
    });

    // Update user status
    this.userStatus.set(userId, {
      online: true,
      lastActive: Date.now(),
    });

    // Set up event handlers
    server.addEventListener('message', async event => {
      try {
        await this.handleMessage(sessionId, event.data);
      } catch (error) {
        console.error('Error handling message:', error);
        this.sendError(sessionId, 'Failed to process message');
      }
    });

    server.addEventListener('close', () => {
      this.handleClose(sessionId);
    });

    server.addEventListener('error', () => {
      this.handleClose(sessionId);
    });

    // Send message history to the new client
    await this.sendMessageHistory(sessionId);

    // Notify other users about this user joining
    await this.broadcastUserStatus(userId, true);

    // Return the client WebSocket
    return new Response(null, {
      status: 101,
      webSocket: client,
    });
  }

  /**
   * Handles incoming messages from WebSocket clients.
   * @param {string} sessionId - The session ID
   * @param {string} data - The message data
   */
  async handleMessage(sessionId, data) {
    // Get the session
    const session = this.sessions.get(sessionId);
    if (!session) {
      return;
    }

    // Parse the message
    let message;
    try {
      message = JSON.parse(data);
    } catch (error) {
      this.sendError(sessionId, 'Invalid JSON');
      return;
    }

    // Handle different message types
    switch (message.action) {
      case 'send_message':
        await this.handleSendMessage(session, message);
        break;
      case 'join_chat':
        // Already handled during connection
        break;
      case 'leave_chat':
        await this.handleLeaveChat(session);
        break;
      default:
        this.sendError(sessionId, 'Unknown action');
    }
  }

  /**
   * Handles a send_message action.
   * @param {Object} session - The session
   * @param {Object} message - The message
   */
  async handleSendMessage(session, message) {
    // Validate message
    if (!message.content || typeof message.content !== 'string') {
      this.sendError(session.userId, 'Invalid message content');
      return;
    }

    // Trim and limit message content
    const content = message.content.trim();
    if (content.length === 0 || content.length > 2000) {
      this.sendError(session.userId, 'Message content must be between 1 and 2000 characters');
      return;
    }

    // Get user details
    const userJson = await this.env.CHAT_KV.get(`user:${session.userId}`);
    if (!userJson) {
      this.sendError(session.userId, 'User not found');
      return;
    }

    const user = JSON.parse(userJson);

    // Create the message object
    const chatMessage = {
      type: 'message',
      messageId: `msg_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`,
      senderId: session.userId,
      senderName: user.displayName,
      content,
      timestamp: Date.now(),
      chatId: message.chatId,
    };

    // Store the message in history
    this.messageHistory.push(chatMessage);
    if (this.messageHistory.length > this.MAX_HISTORY) {
      this.messageHistory.shift();
    }

    // Persist the message to storage
    await this.state.storage.put(`msg:${chatMessage.messageId}`, chatMessage);

    // Broadcast the message to all connected clients
    this.broadcast(chatMessage);

    // Update session last active time
    session.lastActive = Date.now();
    this.userStatus.set(session.userId, {
      online: true,
      lastActive: Date.now(),
    });
  }

  /**
   * Handles a leave_chat action.
   * @param {Object} session - The session
   */
  async handleLeaveChat(session) {
    // Get the group ID from the Durable Object ID
    const groupId = this.state.id.name.replace('group_', '');

    // Get user details
    const userJson = await this.env.CHAT_KV.get(`user:${session.userId}`);
    if (!userJson) {
      return;
    }

    const user = JSON.parse(userJson);

    // Get group details
    const groupJson = await this.env.CHAT_KV.get(`group:${groupId}`);
    if (!groupJson) {
      return;
    }

    const group = JSON.parse(groupJson);

    // Check if the user is the owner
    if (group.ownerId === session.userId) {
      this.sendError(session.userId, 'Group owner cannot leave the group');
      return;
    }

    // Remove the user from the group
    const memberIndex = group.members.indexOf(session.userId);
    if (memberIndex !== -1) {
      group.members.splice(memberIndex, 1);
      await this.env.CHAT_KV.put(`group:${groupId}`, JSON.stringify(group));
    }

    // Remove from members set
    this.members.delete(session.userId);

    // Create a user_left message
    const leaveMessage = {
      type: 'user_left',
      senderId: session.userId,
      senderName: user.displayName,
      content: `${user.displayName} left the group`,
      timestamp: Date.now(),
      chatId: this.state.id.name,
    };

    // Broadcast the message
    this.broadcast(leaveMessage);

    // Close the WebSocket
    try {
      session.webSocket.close(1000, 'User left chat');
    } catch (error) {
      console.error('Error closing WebSocket:', error);
    }

    // Remove the session
    this.sessions.delete(session.userId);

    // Update user status
    this.userStatus.set(session.userId, {
      online: false,
      lastActive: Date.now(),
    });

    // Remove from user's groups list
    const userGroupsKey = `user_groups:${session.userId}`;
    const userGroupsJson = await this.env.CHAT_KV.get(userGroupsKey);
    if (userGroupsJson) {
      const userGroups = JSON.parse(userGroupsJson);
      const groupIndex = userGroups.indexOf(groupId);
      if (groupIndex !== -1) {
        userGroups.splice(groupIndex, 1);
        await this.env.CHAT_KV.put(userGroupsKey, JSON.stringify(userGroups));
      }
    }
  }

  /**
   * Handles WebSocket close events.
   * @param {string} sessionId - The session ID
   */
  handleClose(sessionId) {
    // Get the session
    const session = this.sessions.get(sessionId);
    if (!session) {
      return;
    }

    // Update user status
    this.userStatus.set(session.userId, {
      online: false,
      lastActive: Date.now(),
    });

    // Remove the session
    this.sessions.delete(sessionId);

    // Broadcast user status change
    this.broadcastUserStatus(session.userId, false);
  }

  /**
   * Sends an error message to a specific session.
   * @param {string} sessionId - The session ID
   * @param {string} errorMessage - The error message
   */
  sendError(sessionId, errorMessage) {
    const session = this.sessions.get(sessionId);
    if (!session) {
      return;
    }

    const errorObj = {
      type: 'error',
      content: errorMessage,
      timestamp: Date.now(),
      chatId: this.state.id.name,
    };

    try {
      session.webSocket.send(JSON.stringify(errorObj));
    } catch (error) {
      console.error('Error sending error message:', error);
    }
  }

  /**
   * Broadcasts a message to all connected sessions.
   * @param {Object} message - The message to broadcast
   */
  broadcast(message) {
    const messageStr = JSON.stringify(message);
    
    for (const session of this.sessions.values()) {
      try {
        session.webSocket.send(messageStr);
      } catch (error) {
        console.error('Error broadcasting message:', error);
      }
    }
  }

  /**
   * Broadcasts a user status change to all connected sessions.
   * @param {string} userId - The user ID
   * @param {boolean} online - Whether the user is online
   */
  async broadcastUserStatus(userId, online) {
    // Get user details
    const userJson = await this.env.CHAT_KV.get(`user:${userId}`);
    if (!userJson) {
      return;
    }

    const user = JSON.parse(userJson);

    // Create a status message
    const statusMessage = {
      type: online ? 'user_joined' : 'user_left',
      senderId: userId,
      senderName: user.displayName,
      content: `${user.displayName} ${online ? 'joined' : 'left'} the chat`,
      timestamp: Date.now(),
      chatId: this.state.id.name,
    };

    // Broadcast the message
    this.broadcast(statusMessage);
  }

  /**
   * Sends message history to a specific session.
   * @param {string} sessionId - The session ID
   */
  async sendMessageHistory(sessionId) {
    const session = this.sessions.get(sessionId);
    if (!session) {
      return;
    }

    // Load message history from storage if not already loaded
    if (this.messageHistory.length === 0) {
      const messages = await this.state.storage.list({ prefix: 'msg:' });
      for (const [key, value] of messages) {
        this.messageHistory.push(value);
      }

      // Sort messages by timestamp
      this.messageHistory.sort((a, b) => a.timestamp - b.timestamp);

      // Limit history size
      if (this.messageHistory.length > this.MAX_HISTORY) {
        this.messageHistory = this.messageHistory.slice(-this.MAX_HISTORY);
      }
    }

    // Send each message to the client
    for (const message of this.messageHistory) {
      try {
        session.webSocket.send(JSON.stringify(message));
      } catch (error) {
        console.error('Error sending message history:', error);
      }
    }
  }
}