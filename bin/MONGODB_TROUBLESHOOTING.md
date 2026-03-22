# MongoDB Connection Troubleshooting

## Problem: User Registration Works but Users Not Saved to Database

### Changes Made:
1. **Added @Repository annotation** to UserRepository
2. **Added @Transactional** to AuthService.register() 
3. **Added debug logging** to track user creation
4. **Added debug endpoint** `GET /api/auth/users` to list all users

### Testing Steps:

1. **Check MongoDB Connection:**
   ```bash
   GET {{baseUrl}}/api/auth/users
   ```
   - Should return empty array `[]` initially
   - After registration, should show saved users

2. **Register a User:**
   ```bash
   POST {{baseUrl}}/api/auth/register
   {
       "username": "test_user",
       "password": "password123",
       "email": "test@example.com"
   }
   ```
   - Check console logs for "Registering user: test_user"
   - Check for "Saved user with ID: [ID]"

3. **Verify User Saved:**
   ```bash
   GET {{baseUrl}}/api/auth/users
   ```
   - Should now return array with the registered user

### Possible Issues:

1. **MongoDB Connection Failed:**
   - Check application.properties MONGODB_URI
   - Verify MongoDB Atlas credentials
   - Check network connectivity

2. **Repository Not Scanned:**
   - Added @Repository annotation
   - Ensure @SpringBootApplication is in correct package

3. **Transaction Issues:**
   - Added @Transactional annotation
   - Check MongoDB transaction support

4. **Database Name/Collection Issues:**
   - Default collection: "users" (from @Document annotation)
   - Database name from MONGODB_URI

### Console Logs to Watch:
- "Registering user: [username]"
- "Saving user: [user object]"
- "Saved user with ID: [generated-id]"
- "Total users found: [count]"

### Next Steps:
1. Test with the debug endpoint
2. Check application logs for errors
3. Verify MongoDB Atlas connectivity
4. Consider using local MongoDB for testing
