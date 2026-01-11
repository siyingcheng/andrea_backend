# User Authentication

## User Stories

### Anonymous User

- As an anonymous user, I can register a new account by providing necessary details (username, password, email), so that
I can make purchases and access personalized features.

### Registered User

- As a registered user, I can log in using my username and password, so that I can access my account and personalized features.
- As a registered user, I can log out of my account, so that I can ensure my account's security after using the platform.
- As a registered user, I can reset my password if I forget it by receiving a password reset link via email, so that I can regain access to my account.
- As a registered user, I can update my account information (email, password), so that I can keep my account information up to date.

### Admin User

- As an admin user, I can log in using my admin credentials, so that I can access the admin dashboard and manage the platform.
- As an admin user, I can log out of my admin account, so that I can ensure the security of admin functionalities.
- As an admin user, I can manage user accounts (view, edit, delete users), so that I can manage and maintain the user base effectively.

## Design

### User Model

SQL schema for the User table:

```sql
CREATE TABLE Users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    role ENUM('USER', 'ADMIN') DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP NULL
);
```

Spring Entity for the User model:

```java
@Entity
@Table(name = "Users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false)
    private String passwordHash;
    @Column(unique = true, nullable = false)
    private String email;
    @Enumerated(EnumType.STRING)
    private Role role;
    private Boolean isActive = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    // Getters and Setters
}
public enum Role {
    USER,
    ADMIN
}
```

### API Endpoints (HTTP)

- POST /api/auth/register
  - Description: Register a new user.
  - Request: { username, email, password }
  - Response: 201 Created { id, username, email, role }

- POST /api/auth/login
  - Description: Authenticate user and return access token and refresh token (or set refresh cookie).
  - Request: { username, password }
  - Response: 200 OK { accessToken, expiresIn, tokenType="Bearer" }
  - On success the server should also issue a refresh token. Preferred approach: send refresh token in an HttpOnly Secure SameSite cookie and send access token in response body.

- POST /api/auth/refresh
  - Description: Exchange a valid refresh token for a new access token (and optionally new refresh token).
  - Request: refresh token (cookie or body)
  - Response: 200 OK { accessToken, expiresIn }

- POST /api/auth/logout
  - Description: Invalidate refresh token (server-side) and clear cookie.
  - Request: refresh token (cookie)
  - Response: 204 No Content

- POST /api/auth/password-reset/request
  - Description: Start password reset: send an email with a time-limited tokenized link.
  - Request: { email }
  - Response: 200 OK { message: "If the email exists, a reset link has been sent." }

- POST /api/auth/password-reset/confirm
  - Description: Confirm password reset using token.
  - Request: { token, newPassword }
  - Response: 200 OK { message: "Password updated" }

- GET /api/users/me
  - Description: Get current authenticated user profile.
  - Auth: Bearer token
  - Response: 200 OK { id, username, email, role, createdAt, lastLogin }

- PATCH /api/users/me
  - Description: Update user profile (email, password).
  - Auth: Bearer token
  - Request: partial { email?, currentPassword? (when changing password), newPassword? }
  - Response: 200 OK { updated fields }

- GET /api/admin/users
  - Description: Admin: list users with pagination.
  - Auth: Admin role required
  - Response: 200 OK { items: [User], total, page, size }

- GET /api/admin/users/{id}
  - Description: Admin: view user detail
  - Auth: Admin role required

- PATCH /api/admin/users/{id}
  - Description: Admin: update user (role, isActive)
  - Auth: Admin role required

- DELETE /api/admin/users/{id}
  - Description: Admin: delete (soft delete) user
  - Auth: Admin role required


### Authentication Flows

1) Registration
- Validate input, ensure username/email uniqueness, store password hash (bcrypt) and create inactive or active account (depending on verification strategy).
- Optionally send an email verification token.

2) Login
- Validate credentials: find user by username or email, verify password using bcrypt compare.
- If credentials valid and account is active, update lastLogin timestamp, issue access token and refresh token.
- Access token: short-lived JWT (recommended 10-30 minutes). Refresh token: long-lived opaque token (stored hashed in DB) or long-lived signed JWT; rotate refresh tokens on use.

3) Token Refresh
- Client sends refresh token (cookie or body). Server validates (lookup hashed token), issues new access token and rotates refresh token.
- If refresh token is invalid, revoke session and require re-login.

4) Logout
- Remove or mark refresh token(s) as revoked in DB. Clear refresh cookie.

5) Password Reset
- User requests reset with email. Generate single-use time-limited token (cryptographically random), persist with expiry and send link to email.
- When user submits new password with token, validate token and expiry, update password hash, invalidate token and revoke refresh tokens.


### Security

- Passwords: use bcrypt with a work factor/cost >= 12. Never store plaintext passwords.
- Tokens:
  - Access tokens: JWT signed with strong algorithm (RS256 recommended) or HMAC (HS256) with strong secret. Short lifetime (10-30m).
  - Refresh tokens: store a hashed token in DB and issue an opaque token value to client (or rotate signed refresh tokens). Mark as revoked on logout or use.
  - Store refresh token as HttpOnly Secure SameSite cookie when used from browsers to mitigate XSS.
- Rate limiting: apply per-IP and per-account rate limiting on login, register, and password reset endpoints to mitigate brute force and abuse.
- Account lockout: after N consecutive failed login attempts (configurable, e.g., 5 attempts), lock account for a cooldown period and notify user by email.
- Email verification: optional but recommended to prevent abuse. Use time-limited signed tokens.
- CSRF: when using cookies for auth, protect state-changing POST/PATCH/DELETE endpoints with CSRF tokens.
- CORS: configure allowed origins and ensure credentials are only allowed for trusted origins.
- Logging & Monitoring: log authentication events (success login, failed login, password reset requested) with caution (do not log passwords or tokens). Monitor for suspicious patterns.


### Data Contracts (JSON)

- Registration request
  - POST /api/auth/register
  - Body:
    {
      "username": "alice",
      "email": "alice@example.com",
      "password": "Str0ngP@ssw0rd"
    }

- Login request
    {
      "username": "alice",
      "password": "Str0ngP@ssw0rd"
    }

- Login success response
    {
      "accessToken": "eyJ...",
      "expiresIn": 900,
      "tokenType": "Bearer"
    }

- Error response (standard)
    {
      "timestamp": "2026-01-11T12:00:00Z",
      "status": 400,
      "error": "Bad Request",
      "code": "INVALID_PASSWORD",
      "message": "Password must be at least 8 characters"
    }


### Validation Rules

- username: 3-50 characters; letters, numbers, underscores; unique.
- email: RFC5322-ish regex; unique.
- password: minimum 8 characters; encourage at least one upper, one lower, one digit and one symbol (enforce or provide strength meter).
- All input should be validated server-side and return helpful error messages.


### Error Handling and HTTP Status Codes

- 200 OK: successful GET/POST/UPDATE that returns a body.
- 201 Created: resource created (e.g., registration).
- 204 No Content: successful action with no body (logout).
- 400 Bad Request: validation errors; include a list of field errors.
- 401 Unauthorized: authentication missing or invalid (invalid token).
- 403 Forbidden: authenticated but insufficient permissions (e.g., non-admin calling admin endpoint).
- 404 Not Found: resource not found (e.g., user id).
- 409 Conflict: conflict like duplicate username/email on registration.
- 429 Too Many Requests: rate limiting triggered.
- 500 Internal Server Error: unexpected errors (log and alert).

Standardize error payloads with an error code and user-facing message. Avoid leaking sensitive info (e.g., when requesting password reset, respond with the same message regardless of whether email exists).


### Database Additions

Recommended tables/columns beyond `Users`:

- RefreshTokens
  - id (PK), user_id (FK), token_hash, issued_at, expires_at, revoked BOOLEAN, replaced_by_token (nullable)

- PasswordResetTokens
  - id, user_id, token_hash, created_at, expires_at, used BOOLEAN

- EmailVerificationTokens (optional)
  - id, user_id, token_hash, created_at, expires_at, used BOOLEAN

Implement appropriate indexes (user_id, token_hash) and TTL purging for expired tokens.


### Implementation Notes (Spring Boot)

- Use Spring Security for securing endpoints.
- Implement a UserDetailsService that loads by username or email.
- Use BCryptPasswordEncoder for hashing.
- Implement filters for JWT validation on each request.
- Keep secrets (JWT keys, bcrypt config) in config and not in source control; use environment variables for production.


### Tests

- Unit tests:
  - UserService: register, login (password verification), update profile, password reset flow.
  - TokenService: issue/validate/rotate refresh tokens.
  - Controller layer: input validation and mapping of service errors to HTTP codes.

- Integration tests:
  - Full registration -> login -> access protected endpoint -> refresh token -> logout flow.
  - Password reset end-to-end (token generation and confirmation).

- Security tests:
  - Attempt invalid tokens, expired tokens, token reuse (ensure rotation revokes old refresh tokens).
  - Rate limiting and lockout behaviours.

Provide test data fixtures and use an in-memory DB (H2) for fast CI runs.


## Acceptance Criteria

Map each user story to verifiable acceptance tests:

- Anonymous User / Register
  - Given valid username/email/password, when POST /api/auth/register called, then 201 Created and user row exists with hashed password.
  - Given existing username/email, expect 409 Conflict.

- Registered User / Login
  - Given valid credentials, when POST /api/auth/login called, then 200 OK with access token and refresh token issued and lastLogin updated.
  - Given invalid credentials, 401 Unauthorized; after N failed attempts, account locked and 403 Forbidden returned.

- Registered User / Logout
  - Given valid refresh token, POST /api/auth/logout invalidates refresh token and clears cookie; subsequent refresh attempts fail.

- Registered User / Password Reset
  - Requesting reset returns 200 and creates a single-use token; confirming with token updates password and revokes refresh tokens.

- Registered User / Update Profile
  - Authenticated user can PATCH /api/users/me to update email or password (requires current password when changing password).

- Admin User
  - Admin endpoints require ADMIN role and can list, view, update (role/isActive), and soft-delete users.


### Non-functional Requirements

- Authentication endpoints must respond within acceptable latency (e.g., <300ms under normal load).
- System must support horizontal scaling: token validation should be stateless for access tokens; refresh tokens require a shared store or DB.
- Logging must be GDPR/privacy-aware: do not log passwords or token values.


### Edge Cases and Notes

- Concurrent password reset requests: generate unique tokens and mark used only when consumed. Expire old tokens after a short window.
- Token theft: implement token rotation and revocation lists; provide an endpoint to list and revoke active sessions for a user.
- Account merge/conflict: ensure unique constraints prevent duplicates and return clear errors.


### Next Steps / Optional Improvements

- Add email verification as part of registration to prevent spam accounts.
- Support OAuth2 / social login (Google, Apple) as an alternative to local credentials.
- Add 2FA (TOTP) for admin accounts or allow optional 2FA for users.
- Expose an endpoint for users to list and revoke their active sessions.


### Requirements Coverage

- User stories covered: Registration, Login, Logout, Password Reset, Update Profile, Admin management — Done.
- Security and validation rules defined — Done.
- API endpoints and data contracts — Done.
- Tests and acceptance criteria defined — Done.


## References

- OWASP Authentication Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html
- OWASP Password Storage Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
- RFC 6750 (Bearer token usage) and RFC 7519 (JWT)
