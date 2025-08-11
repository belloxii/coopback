package com.gotryt.coop.config;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service
public class JwtProvider {

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] decodedKey = Base64.getDecoder().decode(JwtConstant.SECRET_KEY);
        key = Keys.hmacShaKeyFor(decodedKey);
    }

    public String generateToken(Authentication auth) {
        List<String> roles = auth.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority) // e.g. "ROLE_ADMIN"
            .collect(Collectors.toList());

        // Debug prints
        System.out.println("Generating JWT for auth: " + auth);
        System.out.println("Roles extracted: " + roles);

        String token = Jwts.builder()
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + JwtConstant.JWT_EXPIRATION_MS))
            .claim("memship", auth.getName())
            .claim("roles", roles)  // keep it simple as a list of strings
            .signWith(key)
            .compact();

        System.out.println("Generated JWT Token: " + token);

        return token;
    }

    public String getMemshipFromToken(String token) {
        token = token.replace("Bearer ", "");
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

        // Debug prints
        System.out.println("Parsed JWT Claims: " + claims);
        System.out.println("Roles in token: " + claims.get("roles", List.class));
        System.out.println("Memship in token: " + claims.get("memship", String.class));

        return claims.get("memship", String.class);
    }
}

