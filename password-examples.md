# Password Validation Examples

## ✅ Valid Passwords

These passwords meet all security requirements:

- `MySecureP@ssw0rd` - Contains all required character types
- `C0mpl3x!Password` - Strong mix of characters
- `Str0ng&S@fe123` - Good complexity
- `Ch@ngeM3N0w!` - Secure and memorable
- `Bu1ld3r$2024` - Year-based but complex
- `Gr8M0rn!ng$` - Creative with special chars
- `P@ssw0rd#123` - Meets all requirements

## ❌ Invalid Passwords

These passwords will be rejected:

### Too Short
- `Abc123!` - Only 7 characters (minimum 8)

### Missing Character Types
- `password123` - No uppercase or special characters
- `PASSWORD123` - No lowercase letters
- `MyPassword` - No numbers or special characters
- `12345678` - No letters
- `MyPass123` - No special characters
- `MyPass!!!` - No numbers

### Common Passwords
- `password` - Too common
- `password123` - Common pattern
- `admin` - Too simple
- `qwerty` - Keyboard pattern
- `123456` - Sequential numbers
- `Password123` - Very common pattern
- `letmein` - Dictionary word

### Simple Patterns
- `aaa12345!` - Repeated characters
- `abc123!A` - Sequential letters
- `qwe123!A` - Keyboard pattern
- `123456!A` - Sequential numbers
- `password!` - Common base word

## 🔒 Security Features

The validator checks for:

1. **Length**: 8-72 characters
2. **Character Diversity**: 
   - At least 1 uppercase letter (A-Z)
   - At least 1 lowercase letter (a-z)
   - At least 1 digit (0-9)
   - At least 1 special character (!@#$%^&*)
3. **Pattern Protection**:
   - No common passwords
   - No keyboard patterns (qwerty, asdf)
   - No sequential patterns (123, abc)
   - No excessive repeated characters (aaa, 111)

## 📝 API Usage

### Registration
```json
POST /auth/register
{
  "username": "johndoe",
  "password": "MySecureP@ssw0rd",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "timezone": "America/New_York"
}
```

### Password Reset
```json
POST /auth/reset-password
{
  "token": "abc123...",
  "newPassword": "NewC0mpl3x!P@ss"
}
```

## 🛡️ Benefits

- **Security**: Prevents weak passwords vulnerable to attacks
- **Compliance**: Meets industry security standards
- **User-Friendly**: Clear error messages guide users
- **Comprehensive**: Multi-layer validation approach
- **Performance**: Optimized regex patterns and early termination