package cl.duoc.finance_batch.security;

import io.github.cdimascio.dotenv.Dotenv; 
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    // CARGAMOS EL ARCHIVO .env
    // directory("./") asegura que busque en la raíz del proyecto
    private final Dotenv dotenv = Dotenv.configure().directory("./").ignoreIfMissing().load();

    // LEEMOS LA VARIABLE
    private final String SECRET_KEY_STRING = dotenv.get("JWT_SECRET", 
        "ZXN0YV9lc191bmFfY2xhdmVfbXV5X3NlZ3VyYV95X2xhcmdhX3BhcmFfY3VtcGxpcl9jb25fbG9zX3JlcXVpc2l0b3NfZGVfSFM1MTJfYmZmXzIwMjZfZHVvY19jaGlsZV9wYXJhX2VsX2V4YW1lbg==");
    
    // El resto es igual...
    private final Key SECRET_KEY = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY_STRING));
    private final long EXPIRATION_TIME = 1000 * 60 * 30; // 30 minutos

    // --- MÉTODOS (IGUAL QUE ANTES) ---
    
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token).getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}