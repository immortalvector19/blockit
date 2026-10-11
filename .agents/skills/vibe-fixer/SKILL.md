---
name: vibe-fixer
description: >-
  Audit, secure, and fix AI-generated and "vibe-coded" applications. Eliminates
  "vulnerable vibe-coded spaghetti", secures authentication, enforces Row-Level Security
  (RLS), seals secret leaks, validates inputs, and sets up security gatekeepers.
  Use when asked to "fix vibe code", "vibe fixer", "audit vibe app", "vibe coding security",
  "secure my app", "check ai generated code", or "audit before launch".
---

# Vibe Fixer: Security & Engineering Audit for AI-Generated Code

*Inspired by [Kema Sambondu (@kema_the_engineer)](https://www.instagram.com/kema_the_engineer/) and the Vibe Coder Security Framework.*

> "Nothing is perfectly secure is NOT an excuse to deploy vulnerable vibe-coded spaghetti directly to the internet."

Vibe coding with AI agents (Claude Code, Cursor, Lovable, Bolt, Replit, Windsurf) allows founders and developers to build apps at record speed. However, AI models optimize for *immediate functional output* rather than *defensive security and architectural resilience*. 

The **Vibe Fixer** does not slow you down or rewrite everything from scratch. It acts as an elite cybersecurity auditor and pair-engineering guardrail: systematically scanning, triaging, and hardening vibe-coded apps into production-ready software.

---

## The Vibe Fixer Philosophy

1. **Velocity + Defense**: Keep the speed of AI development, but enforce non-negotiable security boundaries before production.
2. **Never Trust AI Defaults**: AI routinely leaves admin routes unlocked, leaks service keys in client bundles, forgets database Row-Level Security (RLS), and skips backend input validation.
3. **Plain English + Actionable Fixes**: Every vulnerability finding must include a clear explanation of the real-world exploit risk, a prioritized severity rating, and either a direct code patch or a copy-paste AI prompt to fix it.
4. **Gatekeepers in CI/CD**: Replace "hope it's secure" with automated verification gatekeepers (Playwright CLI, Strix, Trivy, Gitleaks, Supabase CLI).

---

## 8-Pillar Vibe Fixer Audit Protocol

Whenever reviewing a vibe-coded codebase, audit across all 8 pillars in order:

```
┌────────────────────────┐     ┌────────────────────────┐     ┌────────────────────────┐
│ 1. Secrets & Env       │ ──> │ 2. AuthN & AuthZ (IDOR)│ ──> │ 3. Database RLS & BaaS │
└────────────────────────┘     └────────────────────────┘     └────────────────────────┘
                                                                           │
┌────────────────────────┐     ┌────────────────────────┐                  │
│ 5. Network, CORS & API │ <── │ 4. Input & Injection   │ <────────────────┘
└────────────────────────┘     └────────────────────────┘
            │
            ▼
┌────────────────────────┐     ┌────────────────────────┐     ┌────────────────────────┐
│ 6. Dependencies & CVEs │ ──> │ 7. Automated Scanners  │ ──> │ 8. Remediation Plan    │
└────────────────────────┘     └────────────────────────┘     └────────────────────────┘
```

---

### Pillar 1: Secrets & Environment Hygiene

AI coders frequently hardcode credentials or expose backend service keys to the browser bundle.

*   [ ] **No Service Keys in Client Code**:
    *   *Bug*: Putting Supabase `service_role` key, Stripe Secret Key (`sk_live_...`), Firebase Admin credentials, or OpenAI API keys inside client components or prefixed with `NEXT_PUBLIC_`, `VITE_`, `EXPO_PUBLIC_`, or `REACT_APP_`.
    *   *Fix*: Only `anon` / public publishable keys belong on the client. All privileged operations must run in server-side endpoints, Edge Functions, or Server Actions.
*   [ ] **Environment File Safety**:
    *   *Bug*: `.env`, `.env.local`, or `.env.production` committed to Git.
    *   *Fix*: Ensure `.gitignore` includes all `.env*` files except `.env.example`. Check git history (`git log -S "sk_live"`) to ensure keys weren't committed in earlier commits.
*   [ ] **Hardcoded Fallbacks**:
    *   *Bug*: `const jwtSecret = process.env.JWT_SECRET || "default_secret_123"`.
    *   *Fix*: Throw an explicit fatal error at startup if required environment variables are missing (use a typed env validator like `@t3-oss/env-nextjs` or Envalid).

---

### Pillar 2: Authentication & Authorization (Broken Object Level Authorization / IDOR)

The single most common vibe-coding flaw: checking authentication on the frontend, but leaving the backend endpoint completely open.

*   [ ] **Server-Side Route Protection**:
    *   *Bug*: Hiding an "Admin" button in the UI or relying on a client-side `useEffect` redirect without validating permissions in the API route / Server Action.
    *   *Fix*: Every backend route must extract the session/token, verify its signature, and check user roles before executing any query or mutation.
*   [ ] **Insecure Direct Object References (IDOR / BOLA)**:
    *   *Bug*: `DELETE /api/documents/:id` deletes the document using only the `id` from the URL, allowing User A to delete User B's documents by changing the ID.
    *   *Fix*: Always scope queries to the authenticated user:
        ```typescript
        // VULNERABLE VIBE CODE:
        await db.documents.delete({ where: { id: req.params.id } });

        // VIBE FIXER HARDENED:
        await db.documents.delete({
          where: { id: req.params.id, ownerId: req.user.id }
        });
        ```
*   [ ] **Secure Cookie Configuration**:
    *   *Fix*: Auth tokens must be stored in `HttpOnly`, `Secure`, `SameSite=Lax` or `Strict` cookies, never in unencrypted `localStorage` (which is vulnerable to XSS token theft).

---

### Pillar 3: Database & Storage Row-Level Security (RLS)

Platforms like Supabase and Firebase make it easy to prototype, but vibe coders often leave tables with public read/write access.

*   [ ] **Row-Level Security (RLS) Enabled on ALL Tables**:
    *   *Bug*: Creating a table in PostgreSQL/Supabase without `ALTER TABLE tablename ENABLE ROW LEVEL SECURITY;`.
    *   *Fix*: Enable RLS on every single table without exception. Unauthenticated API requests with the `anon` key can read/write entire database tables if RLS is absent.
*   [ ] **Strict RLS Policies**:
    *   *Bug*: Creating a policy like `CREATE POLICY "Allow all" ON items FOR ALL USING (true);`.
    *   *Fix*: Enforce owner constraints:
        ```sql
        CREATE POLICY "Users can only manage their own records"
        ON profiles FOR ALL
        USING (auth.uid() = user_id)
        WITH CHECK (auth.uid() = user_id);
        ```
*   [ ] **Storage Bucket Permissions**:
    *   *Bug*: Making Supabase/Firebase storage buckets public so anyone can list and download private receipts, contracts, or user uploads.
    *   *Fix*: Make buckets private and issue short-lived signed URLs (`createSignedUrl(path, 60)`).
*   [ ] **`SECURITY DEFINER` Traps**:
    *   *Bug*: Creating database functions with `SECURITY DEFINER` without setting `SET search_path = public`, allowing schema hijacking.

---

### Pillar 4: Input Validation & Injection Hardening

AI models often trust client-provided data directly, leading to SQL injection, XSS, and mass assignment.

*   [ ] **Strict Schema Validation**:
    *   *Fix*: Validate all incoming request bodies, query parameters, and headers using Zod, Valibot, or Pydantic before processing:
        ```typescript
        const UpdateUserSchema = z.object({
          name: z.string().min(1).max(100),
          bio: z.string().max(500).optional(),
        }).strict(); // .strict() blocks unexpected fields (mass assignment)
        ```
*   [ ] **Parameterized Queries**:
    *   *Bug*: String interpolation in SQL (`SELECT * FROM users WHERE email = '${email}'`).
    *   *Fix*: Always use parameterized queries or trusted ORMs (Prisma, Drizzle, Kysely).
*   [ ] **Cross-Site Scripting (XSS)**:
    *   *Bug*: Rendering user-generated markdown or HTML using `dangerouslySetInnerHTML` without sanitization.
    *   *Fix*: Sanitize with DOMPurify (`isomorphic-dompurify`) or sanitize-html before rendering.
*   [ ] **SSRF & Webhook Fetching**:
    *   *Bug*: Server fetching arbitrary URLs passed by users (e.g. "preview link" or "fetch RSS").
    *   *Fix*: Block requests to internal IPs (`127.0.0.1`, `169.254.169.254`, `10.0.0.0/8`, `192.168.0.0/16`).

---

### Pillar 5: Network, CORS & API Guardrails

*   [ ] **CORS Lockdown**:
    *   *Bug*: `Access-Control-Allow-Origin: *` paired with `Access-Control-Allow-Credentials: true`.
    *   *Fix*: Whitelist only your specific production frontend domains.
*   [ ] **Rate Limiting on Critical Endpoints**:
    *   *Fix*: Enforce rate limits (using Upstash Redis, `@upstash/ratelimit`, or express-rate-limit) on login, signup, OTP, password reset, and AI generation endpoints to prevent brute-force attacks and wallet-draining API abuse.
*   [ ] **Webhook Signature Verification**:
    *   *Bug*: Processing Stripe, Clerk, or Resend webhooks by inspecting JSON body without validating cryptographic signatures (`stripe.webhooks.constructEvent(...)`).
    *   *Fix*: Always verify the raw request signature header against the webhook signing secret.
*   [ ] **Error Leakage**:
    *   *Bug*: Returning `res.status(500).json({ error: err.stack, query: err.sql })`.
    *   *Fix*: Log detailed errors on the server; return generic messages (`"Internal server error"`) to the client.

---

### Pillar 6: Dependencies & Supply Chain Integrity

*   [ ] **Scan for Known CVEs**:
    *   Run `npm audit --omit=dev` or `pip-audit` / `safety check` to detect vulnerable dependencies.
*   [ ] **Eliminate Hallucinated Packages**:
    *   Verify that packages imported by AI actually exist in official registries and aren't typosquatting or abandoned malware packages.
*   [ ] **Pin Lockfiles**:
    *   Commit `package-lock.json`, `pnpm-lock.yaml`, or `poetry.lock` to ensure deterministic builds.

---

### Pillar 7: Automated Security Scanners & CI/CD Gatekeepers

Tools recommended by Kema Sambondu and modern AppSec:

1.  **Gitleaks**: Scan for committed secrets:
    ```bash
    gitleaks detect --source . --verbose
    ```
2.  **Trivy**: Comprehensive vulnerability scanner for code, dependencies, and containers:
    ```bash
    trivy fs --severity HIGH,CRITICAL .
    ```
3.  **Playwright CLI**: Automated end-to-end security & flow testing:
    *   Simulate unauthorized access: verify logged-out user accessing `/dashboard` gets redirected to `/login`.
    *   Verify User A cannot see User B's dashboard data.
4.  **Strix**: Automated offensive testing simulating hacker exploits against API routes.
5.  **Supabase CLI (Linter)**:
    ```bash
    supabase db lint
    ```
    Identifies missing RLS policies, unindexed foreign keys, and security definer leaks.

---

### Pillar 8: Vibe Fixer Remediation Workflow

When you find vulnerabilities in a project, format the audit report using this structure:

```markdown
# 🛡️ Vibe Fixer Security Audit Report

## Executive Summary
- **Overall Posture**: [Critical | At Risk | Moderate | Production Ready]
- **High-Risk Vulnerabilities**: [Count]
- **Key Risk**: [1-sentence plain English summary of the biggest hazard]

---

## 🚨 Critical Findings & Fixes

### Finding 1: [Vulnerability Name, e.g., Unprotected Admin Route / IDOR]
- **Severity**: CRITICAL
- **Location**: `src/app/api/users/[id]/route.ts`
- **What’s Wrong (Plain English)**: Anyone who knows another user's ID can read or delete their data because the server doesn't check who is making the request.
- **The Exploit**: An attacker loops through IDs `1..1000` and extracts all user records.
- **Direct Code Fix**:
```typescript
// Replace lines 12-18 with:
...
```
- **Copy-and-Paste AI Fix Prompt**:
> "In `src/app/api/users/[id]/route.ts`, add session validation using our auth helper. Ensure that the authenticated user's ID matches the requested user ID, or that the user has an admin role. Return a 403 Forbidden status if unauthorized. Do not change the existing response data format."

---

## 📋 Security Gatekeeper Checklist for Deployment
- [ ] No secrets in client bundle
- [ ] RLS enabled on all database tables
- [ ] Rate limiting active on auth endpoints
- [ ] Webhook signatures cryptographically verified
- [ ] All inputs validated with Zod/Pydantic schemas
```

---

## Pre-Deployment Gatekeeper Script

Provide the user with this pre-deploy verification script (`scripts/vibe-gatekeeper.sh` or `.js`):

```bash
#!/usr/bin/env bash
set -e

echo "🔍 Running Vibe Fixer Pre-Deploy Gatekeeper..."

# 1. Check for secret leaks
echo "1. Checking for exposed API keys..."
if command -v gitleaks &> /dev/null; then
    gitleaks detect --no-git --verbose
else
    echo "⚠️ gitleaks not installed. Skipping local secret scan."
fi

# 2. Dependency Audit
echo "2. Auditing dependencies..."
npm audit --omit=dev --audit-level=high

# 3. Typecheck & Tests
echo "3. Verifying types and running tests..."
npm run build --dry-run || tsc --noEmit
npm test

echo "✅ All gatekeeper checks passed. Vibe code is hardened and ready to ship!"
```
