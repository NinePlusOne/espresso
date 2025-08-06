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

**Important for CI/CD**: If you're using Cloudflare's CI system, make sure the deployment command is set to `npx wrangler deploy` (not `npx wrangler versions upload`) for the initial deployment with Durable Objects.

## Deployment Issues and Solutions

### Issue 1: KV Namespace (RESOLVED ✅)
~~The deployment was failing with: `KV namespace 'your-kv-namespace-id' is not valid`~~
**Status**: Fixed - KV namespace is now properly configured with ID `649a3abcf5a54532a150a894451e28c5`

### Issue 2: Durable Object Migration (NEEDS CI CONFIGURATION CHANGE)
The deployment is failing with:
```
Version upload failed. You attempted to upload a version of a Worker that includes a Durable Object migration, but migrations must be fully applied by running "wrangler deploy".
```

**Root Cause**: The CI system uses `wrangler versions upload` which doesn't support Durable Object migrations.

**Solution Required**: The CI deployment command needs to be changed from `npx wrangler versions upload` to `npx wrangler deploy` for the initial deployment with Durable Objects.

**Why migrations are needed**: Durable Objects require an initial migration to register the new classes, even for the first deployment.

## What's Working

✅ Durable Objects are properly exported and recognized
✅ Worker name matches CI expectations ("project-espresso")  
✅ KV namespace is properly configured
✅ Code syntax and structure are valid
✅ Durable Object migrations are properly configured

## What Needs to be Fixed

❌ CI deployment command needs to be changed from `wrangler versions upload` to `wrangler deploy`

## Future Migrations

If you need to modify Durable Object classes in the future, you'll need to:

1. Add a migration section to `wrangler.toml`
2. Use `wrangler deploy` instead of `wrangler versions upload`
3. Apply migrations before using version uploads

Example migration:
```toml
[[migrations]]
tag = "v2"
new_classes = ["NewDurableObjectClass"]
renamed_classes = [
  { from = "OldClassName", to = "NewClassName" }
]
```