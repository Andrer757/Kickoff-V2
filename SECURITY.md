# Security Policy

## Supported Versions

Security updates and critical vulnerability patches are actively provided for the following releases:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

---

## Reporting a Vulnerability

We take the security of Kickoff, scaffolded projects, and our developer community seriously. If you discover a security vulnerability, please report it responsibly.

### 1. Preferred Method: GitHub Private Security Advisories
Report the issue directly through GitHub's private vulnerability reporting feature:
- Navigate to [**Security -> Report a vulnerability**](https://github.com/Andrer757/Kickoff-V2/security/advisories/new).
- Provide detailed steps to reproduce, impact assessment, and any proof-of-concept code.

### 2. Alternative Method: Direct Email
If you prefer email, contact the maintainer directly:
- **Email**: [andre.rosa@simdea.pt](mailto:andre.rosa@simdea.pt)
- **Subject**: `[SECURITY VULNERABILITY] Kickoff - <Brief Description>`

> **Please DO NOT open public GitHub issues for suspected security vulnerabilities.**

---

## Response & Disclosure Process

1. **Initial Acknowledgment**: We will acknowledge receipt of your report within **48 hours**.
2. **Assessment & Verification**: We will reproduce the issue, determine its impact, and establish severity.
3. **Remediation**: We will author and test a fix in a private branch or advisory fork.
4. **Coordinated Disclosure**: Once a patch is released (via a new patch release and Maven Central artifact), we will publish the advisory crediting the reporter (unless anonymity is requested).

---

## Scope & Guidelines

- **In Scope**:
  - Code generation logic in `:kickoff-cli` resulting in insecure defaults, arbitrary code execution, or path traversal.
  - JSON-RPC handling in the built-in Model Context Protocol (`kickoff mcp`) server.
  - Static analysis vulnerabilities or bypasses in the `quality-verifier` Gradle plugin.
  - Insecure dependency defaults or network leak risks in uncompressed templates (`templates/now-in-android`, `templates/android-sdk`).
- **Out of Scope**:
  - Issues in third-party runtime dependencies after project generation that are outside Kickoff's version catalog.
  - Social engineering or physical access attacks against developer machines.
