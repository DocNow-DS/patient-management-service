# Authentication Guide

## Problem Fixed
The 403 error when accessing patient profile endpoints was caused by missing JWT authentication filter. Spring Security couldn't validate the JWT tokens sent in the Authorization header.

## Solution Implemented
1. **JwtAuthenticationFilter** - Created JWT filter to validate tokens
2. **SecurityConfig** - Updated to include JWT filter in security chain
3. **Postman Collection** - Added auto token extraction script

## How to Use

### 1. Register a User
```bash
POST {{baseUrl}}/api/auth/register
Content-Type: application/json

{
    "username": "john_doe",
    "password": "password123", 
    "email": "john.doe@example.com",
    "roles": ["PATIENT"]
}
```

### 2. Login (Auto-saves token)
```bash
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
    "username": "john_doe",
    "password": "password123"
}
```

The Postman collection automatically extracts and saves the JWT token to `{{jwtToken}}` variable.

### 3. Access Protected Endpoints
```bash
GET {{baseUrl}}/api/patient/profile
Authorization: Bearer {{jwtToken}}
```

## Postman Workflow
1. **Register User** - Create new account
2. **Login User** - Authenticate (token auto-saved)
3. **Get Patient Profile** - Now works with 200 status
4. **Update Profile** - Update patient information
5. **Upload Reports** - Upload medical files
6. **Get Reports/Prescriptions** - View medical data

## Admin Access
For admin endpoints, register with `"roles": ["ADMIN"]` and the token will be saved to `{{adminJwtToken}}`.

## JWT Token Format
Current implementation uses Base64 encoded: `username:timestamp`

## Security Notes
- JWT tokens are simple Base64 (not production-ready)
- No expiration handling in current implementation
- Consider using proper JWT library for production
