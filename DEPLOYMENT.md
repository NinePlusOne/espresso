# Cloudflare Deployment Setup

## Prerequisites

Before deploying this chat application to Cloudflare Workers, you need to set up the required resources.

## Required Setup Steps

### 1. Create a KV Namespace

The application requires a KV namespace for storing user data, authentication tokens, and chat metadata.

**Option A: Using Wrangler CLI**
```bash
npx wrangler kv namespace create "CHAT_KV"
```

**Option B: Using Cloudflare Dashboard**
1. Go to [Cloudflare Dashboard](https://dash.cloudflare.com)
2. Navigate to Workers & Pages > KV
3. Click "Create namespace"
4. Name it "CHAT_KV" or any descriptive name
5. Copy the namespace ID

### 2. Update wrangler.toml

Replace the placeholder KV namespace ID in `wrangler.toml`:

```toml
kv_namespaces = [
  { binding = "CHAT_KV", id = "your-actual-kv-namespace-id-here" }
]
```

### 3. Deploy

Once the KV namespace is configured, deploy using:

```bash
npx wrangler deploy
```

## Current Deployment Issue

The deployment is currently failing with:
```
KV namespace 'your-kv-namespace-id' is not valid. [code: 10042]
```

This is because the `wrangler.toml` file contains a placeholder value instead of a real KV namespace ID.

## What's Working

✅ Durable Objects are now properly exported and recognized
✅ Worker name matches CI expectations ("project-espresso")
✅ Code syntax and structure are valid

## Next Steps

1. Create a KV namespace (see steps above)
2. Update the namespace ID in `wrangler.toml`
3. Redeploy the application

The application will be fully functional once the KV namespace is properly configured.