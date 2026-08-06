package ma.onee.dsi.projectportfolio.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key:ZTk4N2Q2YzViNGEzMmYxMGQ5ODc2YzViNGEzMmYxMGQ5ODc2YzViNGEzMmYxMGQ5ODc2YzViNGEzMmYxMA==}")
    private String secretKey;

    @Value("${application.security.jwt.expiration:86400000}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.getSubject());
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(Utilisateur utilisateur) {
        return generateToken(new HashMap<>(), utilisateur);
    }

    public String generateToken(Map<String, Object> extraClaims, Utilisateur utilisateur) {
        return buildToken(extraClaims, utilisateur.getEmail(), jwtExpiration);
    }

    public String generateToken(Map<String, Object> extraClaims, String subject) {
        return buildToken(extraClaims, subject, jwtExpiration);
    }

    public boolean isTokenValid(String token, Utilisateur utilisateur) {
        final String username = extractUsername(token);
        return username.equals(utilisateur.getEmail()) && !isTokenExpired(token);
    }

    public boolean isTokenValid(String token, String subject) {
        final String username = extractUsername(token);
        return username.equals(subject) && !isTokenExpired(token);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            String subject,
            long expiration
    ) {
        Date issuedAt = new Date();
        Date expiryDate = new Date(issuedAt.getTime() + expiration);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(issuedAt)
                .expiration(expiryDate)
                .signWith(getSignInKey())
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, claims -> claims.getExpiration());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
