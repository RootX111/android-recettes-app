# CVE-2026-29000: JWT Authentication Bypass in pac4j-jwt

## Vulnerability Summary

**CVE ID:** CVE-2026-29000
**Severity:** CRITICAL
**CVSS Score:** 9.8 (estimated)
**CWE:** CWE-287 (Improper Authentication)

### Affected Versions
- pac4j-jwt < 4.5.9
- pac4j-jwt 5.x < 5.7.9
- pac4j-jwt 6.x < 6.3.3

### Description

pac4j-jwt versions prior to 4.5.9, 5.7.9, and 6.3.3 contain a critical authentication bypass vulnerability in the `JwtAuthenticator` component when processing encrypted JWTs (JSON Web Encryption - JWE).

The vulnerability allows remote attackers who possess the server's RSA public key to forge authentication tokens by creating a JWE-wrapped PlainJWT with arbitrary subject and role claims. This bypasses signature verification entirely, allowing attackers to authenticate as any user, including administrators.

## Technical Details

### Root Cause

The vulnerable `JwtAuthenticator` in pac4j-jwt improperly handles JWE-encrypted JWT tokens. The authentication flow has the following flaw:

1. Server receives a JWE token from the client
2. Server decrypts the JWE using its RSA private key
3. Server extracts the inner JWT payload
4. **VULNERABILITY**: Server fails to verify the signature of the inner JWT
5. Server accepts the claims from the unsigned JWT

### Attack Vector

An attacker who obtains the server's RSA public key (which is often publicly available or easily obtainable) can:

1. Create a PlainJWT (unsigned JWT with `alg: none`) containing arbitrary claims:
   - `sub`: Target username (e.g., "admin")
   - `roles`: Desired roles (e.g., ["admin", "superuser"])
   - Any other claims needed for authentication

2. Wrap the PlainJWT in JWE encryption using the server's public key:
   - Use RSA-OAEP algorithm to encrypt a Content Encryption Key (CEK)
   - Use the CEK with AES-GCM to encrypt the PlainJWT
   - Construct the JWE: `header.encrypted_key.iv.ciphertext.tag`

3. Send the forged JWE token to the server

4. The vulnerable server:
   - Decrypts the JWE
   - Extracts the PlainJWT
   - **Accepts the unsigned JWT without verification**
   - Grants authentication and authorization based on the forged claims

### Impact

- **Complete Authentication Bypass**: Attackers can authenticate as any user
- **Privilege Escalation**: Attackers can grant themselves administrator privileges
- **Unauthorized Access**: Access to sensitive data and administrative functions
- **Low Attack Complexity**: Only requires the server's public RSA key

## Exploitation

### Prerequisites

- Target server running vulnerable pac4j-jwt version
- Server's RSA public key (often publicly available)
- Python 3.x with `cryptography` library

### Running the Exploit

```bash
# Install dependencies
pip install cryptography

# Run the exploit demonstration
python3 exploit.py
```

### Exploit Output

The exploit demonstrates:
1. Creation of PlainJWT with arbitrary claims
2. JWE wrapping process (conceptual)
3. Multiple attack scenarios:
   - Administrator impersonation
   - User impersonation
   - Privilege escalation

### Example Attack Scenario

```python
from exploit import CVE202629000Exploit

# Initialize with target server's public key
with open('server_public_key.pem', 'r') as f:
    public_key = f.read()

exploit = CVE202629000Exploit(public_key)

# Create token to impersonate admin
forged_token = exploit.create_exploit_token(
    target_user="admin",
    target_roles=["admin", "superuser"]
)

# Use forged_token to authenticate to the vulnerable server
```

## Proof of Concept Flow

```
┌─────────────┐
│   Attacker  │
└──────┬──────┘
       │ 1. Obtains server's RSA public key
       │
       ▼
┌─────────────────────────────────────┐
│ Create PlainJWT (alg: none)         │
│ {                                   │
│   "sub": "admin",                   │
│   "roles": ["admin", "superuser"],  │
│   "iat": 1710560000,                │
│   "exp": 1742096000                 │
│ }                                   │
└──────┬──────────────────────────────┘
       │ 2. Wrap in JWE encryption
       │    using server's public key
       ▼
┌─────────────────────────────────────┐
│ JWE Token:                          │
│ header.encrypted_key.iv.ciphertext.tag │
└──────┬──────────────────────────────┘
       │ 3. Send to server
       │
       ▼
┌─────────────────────────────────────┐
│ Vulnerable pac4j-jwt Server         │
│ - Decrypts JWE with private key     │
│ - Extracts PlainJWT                 │
│ - FAILS to verify signature         │
│ - Accepts forged claims             │
│ - Grants admin access               │
└──────┬──────────────────────────────┘
       │ 4. Access granted!
       ▼
┌─────────────────────────────────────┐
│ Attacker has administrator access   │
└─────────────────────────────────────┘
```

## Mitigation

### Immediate Actions

1. **Update pac4j-jwt immediately** to one of the following versions:
   - Version 4.5.9 or later (for 4.x branch)
   - Version 5.7.9 or later (for 5.x branch)
   - Version 6.3.3 or later (for 6.x branch)

2. **Reject unsigned JWTs**: Configure JWT validation to reject tokens with `alg: none`

3. **Enforce signature verification**: Ensure all JWT tokens are cryptographically verified

### Long-term Solutions

1. **Defense in Depth**:
   - Always verify JWT signatures, even after JWE decryption
   - Implement allowlist of accepted signature algorithms
   - Reject `alg: none` explicitly in configuration

2. **Key Management**:
   - Rotate RSA keys regularly
   - Keep private keys secure
   - Consider limiting public key distribution

3. **Monitoring**:
   - Log all authentication attempts
   - Monitor for unusual JWT patterns
   - Alert on JWTs with `alg: none`

## Patched Versions

The vulnerability has been fixed in:
- **pac4j-jwt 4.5.9+**: Enforces signature verification after JWE decryption
- **pac4j-jwt 5.7.9+**: Enforces signature verification after JWE decryption
- **pac4j-jwt 6.3.3+**: Enforces signature verification after JWE decryption

### Fix Details

The patch ensures that:
1. JWE decryption is followed by signature verification
2. PlainJWT tokens (`alg: none`) are explicitly rejected
3. Only properly signed JWTs are accepted for authentication

## References

- **CWE-287**: Improper Authentication
  - https://cwe.mitre.org/data/definitions/287.html

- **JWT Best Practices**: RFC 8725
  - https://datatracker.ietf.org/doc/html/rfc8725

- **JWE Specification**: RFC 7516
  - https://datatracker.ietf.org/doc/html/rfc7516

- **pac4j-jwt Project**
  - https://github.com/pac4j/pac4j

## Timeline

- **2026-03-16**: Vulnerability documented and exploit created
- **2026-XX-XX**: Responsible disclosure to pac4j maintainers
- **2026-XX-XX**: Patch released in versions 4.5.9, 5.7.9, 6.3.3
- **2026-XX-XX**: CVE-2026-29000 published

## Credits

Security Research Team

## Disclaimer

This exploit is provided for educational and authorized security testing purposes only. Unauthorized access to computer systems is illegal. Always obtain proper authorization before conducting security assessments.

## License

This proof-of-concept is provided for educational purposes under responsible disclosure guidelines.
