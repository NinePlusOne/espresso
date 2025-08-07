/**
 * Main worker script for the chat application.
 * Handles HTTP API requests and WebSocket connections.
 */

import { DMRoom } from './dm-room.js';
import { GroupRoom } from './group-room.js';

// Export Durable Object classes for Cloudflare Workers runtime
export { DMRoom, GroupRoom };

// Constants
const JWT_SECRET = 'your-jwt-secret-key-change-this-in-production';
const JWT_EXPIRATION = 60 * 60 * 24; // 24 hours in seconds
const RATE_LIMIT_MAX = 100; // Maximum requests per minute
const RATE_LIMIT_WINDOW = 60 * 1000; // 1 minute in milliseconds

/**
 * Main request handler for the worker.
 */
export default {
  /**
   * Fetch event handler.
   * @param {Request} request - The incoming request
   * @param {Object} env - Environment variables and bindings
   * @param {Object} ctx - Execution context
   * @returns {Response} The response
   */
  async fetch(request, env, ctx) {
    // Set up CORS headers
    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
      'Access-Control-Max-Age': '86400',
    };

    // Handle preflight requests
    if (request.method === 'OPTIONS') {
      return new Response(null, {
        headers: corsHeaders,
      });
    }

    // Apply rate limiting
    const rateLimited = await this.applyRateLimit(request, env);
    if (rateLimited) {
      return new Response('Rate limit exceeded', { status: 429 });
    }

    // Get the URL and pathname
    const url = new URL(request.url);
    const path = url.pathname;

    // Add CORS headers to all responses
    const headers = new Headers(corsHeaders);
    headers.set('Content-Type', 'application/json');

    try {
      // Handle WebSocket upgrade
      if (path === '/ws') {
        return this.handleWebSocketUpgrade(request, env);
      }

      // Handle API routes
      if (path.startsWith('/api/')) {
        return this.handleApiRequest(request, env, path, headers);
      }

      // Handle unknown routes
      return new Response(JSON.stringify({ error: 'Not found' }), {
        status: 404,
        headers,
      });
    } catch (error) {
      console.error('Worker error:', error);
      return new Response(JSON.stringify({ error: 'Internal server error' }), {
        status: 500,
        headers,
      });
    }
  },

  /**
   * Applies rate limiting to requests.
   * @param {Request} request - The incoming request
   * @param {Object} env - Environment variables and bindings
   * @returns {boolean} True if rate limited, false otherwise
   */
  async applyRateLimit(request, env) {
    // Get client IP
    const clientIp = request.headers.get('CF-Connecting-IP') || 'unknown';
    const key = `ratelimit:${clientIp}`;

    // Get current count from KV
    let count = 0;
    try {
      const storedCount = await env.CHAT_KV.get(key);
      if (storedCount) {
        count = parseInt(storedCount, 10);
      }
    } catch (error) {
      console.error('Rate limit KV error:', error);
      return false; // Don't rate limit on errors
    }

    // Check if rate limit exceeded
    if (count >= RATE_LIMIT_MAX) {
      return true;
    }

    // Increment count
    await env.CHAT_KV.put(key, (count + 1).toString(), {
      expirationTtl: RATE_LIMIT_WINDOW / 1000,
    });

    return false;
  },

  /**
   * Handles WebSocket upgrade requests.
   * @param {Request} request - The incoming request
   * @param {Object} env - Environment variables and bindings
   * @returns {Response} The WebSocket response
   */
  async handleWebSocketUpgrade(request, env) {
    // Check for WebSocket upgrade
    const upgradeHeader = request.headers.get('Upgrade');
    if (!upgradeHeader || upgradeHeader.toLowerCase() !== 'websocket') {
      return new Response('Expected WebSocket', { status: 426 });
    }

    // Verify authentication
    const authHeader = request.headers.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return new Response('Unauthorized', { status: 401 });
    }

    const token = authHeader.substring(7);
    const payload = await this.verifyJwt(token);
    if (!payload) {
      return new Response('Invalid token', { status: 401 });
    }

    // Ensure payload contains userId
    if (!payload.userId) {
      console.error('JWT payload missing userId:', payload);
      return new Response('Invalid token: missing userId', { status: 401 });
    }

    // Get chat ID from query parameters
    const url = new URL(request.url);
    const chatId = url.searchParams.get('chatId');
    if (!chatId) {
      return new Response('Missing chatId parameter', { status: 400 });
    }

    // Route to the appropriate Durable Object
    try {
      // Create a new request with the userId in the headers
      const newRequest = new Request(request.url, {
        method: request.method,
        headers: new Headers(request.headers),
        body: request.body,
        redirect: request.redirect,
      });
      
      // Add userId to the headers so the Durable Object can access it
      newRequest.headers.set('X-User-ID', payload.userId);
      
      if (chatId.startsWith('dm_')) {
        // Direct message chat
        const dmId = env.DM_ROOM.idFromName(chatId);
        const dmRoom = env.DM_ROOM.get(dmId);
        return dmRoom.fetch(newRequest);
      } else if (chatId.startsWith('group_')) {
        // Group chat
        const groupId = env.GROUP_ROOM.idFromName(chatId);
        const groupRoom = env.GROUP_ROOM.get(groupId);
        return groupRoom.fetch(newRequest);
      } else {
        return new Response('Invalid chatId format', { status: 400 });
      }
    } catch (error) {
      console.error('Error routing WebSocket request:', error);
      return new Response('Internal server error', { status: 500 });
    }
  },

  /**
   * Handles API requests.
   * @param {Request} request - The incoming request
   * @param {Object} env - Environment variables and bindings
   * @param {string} path - The request path
   * @param {Headers} headers - Response headers
   * @returns {Response} The API response
   */
  async handleApiRequest(request, env, path, headers) {
    // Handle authentication routes
    if (path === '/api/register' && request.method === 'POST') {
      return this.handleRegister(request, env, headers);
    }

    if (path === '/api/login' && request.method === 'POST') {
      return this.handleLogin(request, env, headers);
    }

    // All other routes require authentication
    const authHeader = request.headers.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return new Response(JSON.stringify({ error: 'Unauthorized' }), {
        status: 401,
        headers,
      });
    }

    const token = authHeader.substring(7);
    const payload = await this.verifyJwt(token);
    if (!payload) {
      return new Response(JSON.stringify({ error: 'Invalid token' }), {
        status: 401,
        headers,
      });
    }

    // User routes
    if (path === '/api/users/search' && request.method === 'GET') {
      return this.handleUserSearch(request, env, headers, payload);
    }

    // Group routes
    if (path === '/api/groups' && request.method === 'POST') {
      return this.handleCreateGroup(request, env, headers, payload);
    }

    if (path === '/api/groups/search' && request.method === 'GET') {
      return this.handleGroupSearch(request, env, headers, payload);
    }

    // Handle unknown API routes
    return new Response(JSON.stringify({ error: 'Not found' }), {
      status: 404,
      headers,
    });
  },

  /**
   * Handles user registration.
   * @param {Request} request - The incoming request
   * @param {Object} env - Environment variables and bindings
   * @param {Headers} headers - Response headers
   * @returns {Response} The registration response
   */
  async handleRegister(request, env, headers) {
    try {
      const data = await request.json();
      const { userId, password, displayName } = data;

      // Validate input
      if (!userId || !password || !displayName) {
        return new Response(JSON.stringify({ error: 'Missing required fields' }), {
          status: 400,
          headers,
        });
      }

      // Check if user already exists
      const existingUser = await env.CHAT_KV.get(`user:${userId}`);
      if (existingUser) {
        return new Response(JSON.stringify({ error: 'User already exists' }), {
          status: 409,
          headers,
        });
      }

      // Hash the password
      const passwordHash = await this.hashPassword(password);

      // Create user object
      const user = {
        userId,
        displayName,
        passwordHash,
        createdAt: Date.now(),
      };

      // Store user in KV
      await env.CHAT_KV.put(`user:${userId}`, JSON.stringify(user));

      return new Response(JSON.stringify({ success: true }), {
        status: 201,
        headers,
      });
    } catch (error) {
      console.error('Registration error:', error);
      return new Response(JSON.stringify({ error: 'Invalid request' }), {
        status: 400,
        headers,
      });
    }
  },

  /**
   * Handles user login.
   * @param {Request} request - The incoming request
   * @param {Object} env - Environment variables and bindings
   * @param {Headers} headers - Response headers
   * @returns {Response} The login response
   */
  async handleLogin(request, env, headers) {
    try {
      const data = await request.json();
      const { userId, password } = data;

      // Validate input
      if (!userId || !password) {
        return new Response(JSON.stringify({ error: 'Missing required fields' }), {
          status: 400,
          headers,
        });
      }

      // Get user from KV
      const userJson = await env.CHAT_KV.get(`user:${userId}`);
      if (!userJson) {
        return new Response(JSON.stringify({ error: 'Invalid credentials' }), {
          status: 401,
          headers,
        });
      }

      const user = JSON.parse(userJson);

      // Verify password
      const passwordValid = await this.verifyPassword(password, user.passwordHash);
      if (!passwordValid) {
        return new Response(JSON.stringify({ error: 'Invalid credentials' }), {
          status: 401,
          headers,
        });
      }

      // Generate JWT token
      const token = await this.generateJwt({
        userId,
        displayName: user.displayName,
      });

      return new Response(
        JSON.stringify({
          success: true,
          token,
          userId,
          displayName: user.displayName,
        }),
        {
          status: 200,
          headers,
        }
      );
    } catch (error) {
      console.error('Login error:', error);
      return new Response(JSON.stringify({ error: 'Invalid request' }), {
        status: 400,
        headers,
      });
    }
  },

  /**
   * Handles user search.
   * @param {Request} request - The incoming request
   * @param {Object} env - Environment variables and bindings
   * @param {Headers} headers - Response headers
   * @param {Object} payload - JWT payload
   * @returns {Response} The search response
   */
  async handleUserSearch(request, env, headers, payload) {
    try {
      const url = new URL(request.url);
      const query = url.searchParams.get('q') || '';

      // List all users (in a real app, this would be paginated and filtered)
      const users = [];
      const userKeys = await env.CHAT_KV.list({ prefix: 'user:' });

      for (const key of userKeys.keys) {
        const userJson = await env.CHAT_KV.get(key.name);
        if (userJson) {
          const user = JSON.parse(userJson);
          
          // Filter by query if provided
          if (query && !user.userId.includes(query) && !user.displayName.toLowerCase().includes(query.toLowerCase())) {
            continue;
          }
          
          // Don't include the current user
          if (user.userId === payload.userId) {
            continue;
          }
          
          users.push({
            userId: user.userId,
            displayName: user.displayName,
            online: false, // In a real app, this would be determined from an online status system
          });
        }
      }

      return new Response(JSON.stringify(users), {
        status: 200,
        headers,
      });
    } catch (error) {
      console.error('User search error:', error);
      return new Response(JSON.stringify({ error: 'Failed to search users' }), {
        status: 500,
        headers,
      });
    }
  },

  /**
   * Handles group creation.
   * @param {Request} request - The incoming request
   * @param {Object} env - Environment variables and bindings
   * @param {Headers} headers - Response headers
   * @param {Object} payload - JWT payload
   * @returns {Response} The group creation response
   */
  async handleCreateGroup(request, env, headers, payload) {
    try {
      const data = await request.json();
      const { name, description } = data;

      // Validate input
      if (!name) {
        return new Response(JSON.stringify({ error: 'Group name is required' }), {
          status: 400,
          headers,
        });
      }

      // Generate a unique group ID
      const groupId = `${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;

      // Create group object
      const group = {
        groupId,
        name,
        description: description || '',
        ownerId: payload.userId,
        members: [payload.userId],
        createdAt: Date.now(),
      };

      // Store group in KV
      await env.CHAT_KV.put(`group:${groupId}`, JSON.stringify(group));

      // Add to user's groups
      const userGroupsKey = `user_groups:${payload.userId}`;
      let userGroups = [];
      const userGroupsJson = await env.CHAT_KV.get(userGroupsKey);
      if (userGroupsJson) {
        userGroups = JSON.parse(userGroupsJson);
      }
      userGroups.push(groupId);
      await env.CHAT_KV.put(userGroupsKey, JSON.stringify(userGroups));

      return new Response(JSON.stringify({ groupId }), {
        status: 201,
        headers,
      });
    } catch (error) {
      console.error('Create group error:', error);
      return new Response(JSON.stringify({ error: 'Failed to create group' }), {
        status: 500,
        headers,
      });
    }
  },

  /**
   * Handles group search.
   * @param {Request} request - The incoming request
   * @param {Object} env - Environment variables and bindings
   * @param {Headers} headers - Response headers
   * @param {Object} payload - JWT payload
   * @returns {Response} The search response
   */
  async handleGroupSearch(request, env, headers, payload) {
    try {
      const url = new URL(request.url);
      const query = url.searchParams.get('q') || '';

      // List all groups (in a real app, this would be paginated and filtered)
      const groups = [];
      const groupKeys = await env.CHAT_KV.list({ prefix: 'group:' });

      for (const key of groupKeys.keys) {
        const groupJson = await env.CHAT_KV.get(key.name);
        if (groupJson) {
          const group = JSON.parse(groupJson);
          
          // Filter by query if provided
          if (query && !group.name.toLowerCase().includes(query.toLowerCase()) && 
              !group.description.toLowerCase().includes(query.toLowerCase())) {
            continue;
          }
          
          groups.push({
            groupId: group.groupId,
            name: group.name,
            description: group.description,
            memberCount: group.members.length,
            isOwner: group.ownerId === payload.userId,
            isMember: group.members.includes(payload.userId),
          });
        }
      }

      return new Response(JSON.stringify(groups), {
        status: 200,
        headers,
      });
    } catch (error) {
      console.error('Group search error:', error);
      return new Response(JSON.stringify({ error: 'Failed to search groups' }), {
        status: 500,
        headers,
      });
    }
  },

  /**
   * Generates a JWT token.
   * @param {Object} payload - The payload to include in the token
   * @returns {string} The JWT token
   */
  async generateJwt(payload) {
    // Create JWT header
    const header = {
      alg: 'HS256',
      typ: 'JWT',
    };

    // Ensure payload contains userId
    if (!payload.userId) {
      console.error('Missing userId in JWT payload:', payload);
      throw new Error('Missing userId in JWT payload');
    }

    // Create JWT payload
    const jwtPayload = {
      ...payload,
      exp: Math.floor(Date.now() / 1000) + JWT_EXPIRATION,
      iat: Math.floor(Date.now() / 1000),
    };

    // Encode header and payload
    const encodedHeader = btoa(JSON.stringify(header));
    const encodedPayload = btoa(JSON.stringify(jwtPayload));

    // Create signature
    const data = `${encodedHeader}.${encodedPayload}`;
    const encoder = new TextEncoder();
    const key = await crypto.subtle.importKey(
      'raw',
      encoder.encode(JWT_SECRET),
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['sign']
    );
    const signature = await crypto.subtle.sign('HMAC', key, encoder.encode(data));

    // Convert signature to base64
    const signatureBase64 = btoa(String.fromCharCode(...new Uint8Array(signature)));

    // Return complete JWT
    return `${encodedHeader}.${encodedPayload}.${signatureBase64}`;
  },

  /**
   * Verifies a JWT token.
   * @param {string} token - The JWT token to verify
   * @returns {Object|null} The payload if valid, null otherwise
   */
  async verifyJwt(token) {
    try {
      // Split token into parts
      const parts = token.split('.');
      if (parts.length !== 3) {
        console.error('Invalid JWT format: token does not have three parts');
        return null;
      }

      const [encodedHeader, encodedPayload, signature] = parts;

      // Verify signature
      const data = `${encodedHeader}.${encodedPayload}`;
      const encoder = new TextEncoder();
      const key = await crypto.subtle.importKey(
        'raw',
        encoder.encode(JWT_SECRET),
        { name: 'HMAC', hash: 'SHA-256' },
        false,
        ['verify']
      );

      // Convert base64 signature to ArrayBuffer
      const signatureBytes = atob(signature);
      const signatureArray = new Uint8Array(signatureBytes.length);
      for (let i = 0; i < signatureBytes.length; i++) {
        signatureArray[i] = signatureBytes.charCodeAt(i);
      }

      const valid = await crypto.subtle.verify(
        'HMAC',
        key,
        signatureArray,
        encoder.encode(data)
      );

      if (!valid) {
        console.error('JWT signature verification failed');
        return null;
      }

      // Decode payload
      const payload = JSON.parse(atob(encodedPayload));

      // Check expiration
      if (payload.exp && payload.exp < Math.floor(Date.now() / 1000)) {
        console.error('JWT token expired');
        return null;
      }

      // Verify payload contains userId
      if (!payload.userId) {
        console.error('JWT payload missing userId:', payload);
        return null;
      }

      return payload;
    } catch (error) {
      console.error('JWT verification error:', error);
      return null;
    }
  },

  /**
   * Hashes a password.
   * @param {string} password - The password to hash
   * @returns {string} The hashed password
   */
  async hashPassword(password) {
    // In a real app, you would use a proper password hashing algorithm like bcrypt
    // For this example, we'll use a simple SHA-256 hash with a salt
    const salt = crypto.randomUUID();
    const encoder = new TextEncoder();
    const data = encoder.encode(`${password}${salt}`);
    const hash = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hash));
    const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
    return `${hashHex}.${salt}`;
  },

  /**
   * Verifies a password against a hash.
   * @param {string} password - The password to verify
   * @param {string} storedHash - The stored hash
   * @returns {boolean} True if the password is valid, false otherwise
   */
  async verifyPassword(password, storedHash) {
    // Split the stored hash into hash and salt
    const [hash, salt] = storedHash.split('.');
    
    // Hash the provided password with the same salt
    const encoder = new TextEncoder();
    const data = encoder.encode(`${password}${salt}`);
    const newHash = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(newHash));
    const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
    
    // Compare the hashes
    return hash === hashHex;
  },
};