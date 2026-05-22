package com.f1setups.services;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

public class AuthTokenService
{
    // Token format: base64url(userId:expiresAt).base64url(HMAC signature).
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_TTL_SECONDS = 24 * 60 * 60; // 24 hours
    // Development fallback only. Production should set F1SETUPS_AUTH_SECRET.
    private static final String DEFAULT_SECRET = "dev-only-token-secret-change-before-production";

    private final byte[] secretKey;

    public AuthTokenService()
    {
        this(System.getenv().getOrDefault("F1SETUPS_AUTH_SECRET", DEFAULT_SECRET));
    }

    public AuthTokenService(String secret)
    {
        if (secret == null || secret.isBlank())
        {
            throw new IllegalArgumentException("[AuthTokenService] Token secret cannot be empty");
        }

        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateToken(int userId)
    {
        // Store only the user id and expiration in the token payload.
        long expiresAt = Instant.now().getEpochSecond() + TOKEN_TTL_SECONDS;
        String payload = userId + ":" + expiresAt;
        String encodedPayload = encode(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);

        return encodedPayload + "." + signature;
    }

    public int getUserIdFromAuthorizationHeader(String authorizationHeader) throws Exception
    {
        // Protected controllers should call this instead of trusting a userId from the client.
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer "))
        {
            throw new Exception("[AuthTokenService] Missing bearer token");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        return validateToken(token);
    }

    public int validateToken(String token) throws Exception
    {
        if (token == null || token.isBlank())
        {
            throw new Exception("[AuthTokenService] Token cannot be empty");
        }

        String[] tokenParts = token.split("\\.", 2);
        if (tokenParts.length != 2)
        {
            throw new Exception("[AuthTokenService] Invalid token format");
        }

        String encodedPayload = tokenParts[0];
        String signature = tokenParts[1];
        String expectedSignature = sign(encodedPayload);

        // Compare signatures in constant time to avoid leaking where they differ.
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)))
        {
            throw new Exception("[AuthTokenService] Invalid token signature");
        }

        String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        String[] payloadParts = payload.split(":", 2);
        if (payloadParts.length != 2)
        {
            throw new Exception("[AuthTokenService] Invalid token payload");
        }

        int userId = Integer.parseInt(payloadParts[0]);
        long expiresAt = Long.parseLong(payloadParts[1]);

        if (Instant.now().getEpochSecond() > expiresAt)
        {
            throw new Exception("[AuthTokenService] Token expired");
        }

        return userId;
    }

    private String sign(String value)
    {
        try
        {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            return encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e)
        {
            throw new IllegalStateException("[AuthTokenService] Failed to sign token", e);
        }
    }

    private String encode(byte[] bytes)
    {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
