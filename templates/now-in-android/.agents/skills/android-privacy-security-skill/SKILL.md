---
name: android-privacy-security-skill
description: >
  Guidance for protecting PII (Personally Identifiable Information) and secrets
  in Android applications. Prevents logging leaks and hardcoded credentials.
---

# Android Privacy & Security

Protecting user data and system integrity is a non-negotiable requirement for the Android application ecosystem.

## 1. PII Protection (Logging)

Never log data that can be used to identify a user.

| Data Type | Rule | Alternative |
|-----------|------|-------------|
| **Email / Phone** | **PROHIBITED** | Use a unique hash or masked version (e.g., u***@example.com). |
| **Auth Tokens** | **PROHIBITED** | Log only the presence/absence of the token, not the value. |
| **Location** | **PROHIBITED** | Log only the precision level or success/fail of the provider. |
| **Credit Card** | **PROHIBITED** | Never log. Use payment gateway IDs only. |

### Semantic Scan Rule
Check all `Log.d`, `Log.i`, `println`, and crash reporting events (`FirebaseCrashlytics.log`) for variable names like `email`, `phone`, `password`, `token`, `user_id`.

## 2. Secret Management

- **No Hardcoded Keys:** API keys, secrets, or certificates must never be committed to Git.
- **local.properties:** Use `local.properties` for local development secrets.
- **BuildConfigs:** Inject secrets via Gradle `buildConfigField` or `manifestPlaceholders`.
- **Secure Local Storage:** Sensitive local data must be encrypted using Android Keystore + Jetpack DataStore / Tink (do not use plain-text preferences or deprecated `EncryptedSharedPreferences`).

## 3. Security Checklist

- [ ] Does any log statement contain potentially sensitive user data?
- [ ] Is there any hardcoded string that looks like an API Key or Secret?
- [ ] Are sensitive network responses (containing PII) logged in production builds?
- [ ] Are Room databases containing PII encrypted?
