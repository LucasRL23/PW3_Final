package br.edu.ifsp.prw3.consertos.security;

import br.edu.ifsp.prw3.consertos.usuario.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PW3TokenService {

    private static final String ISSUER = "DISCIPLINA PW3";
    private final Algorithm algoritmo;

    public PW3TokenService(@Value("${pw3.senha.principal.geracao.token}") String secret) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET deve ter pelo menos 32 caracteres.");
        }
        this.algoritmo = Algorithm.HMAC256(secret);
    }

    public String gerarToken(Usuario usuario) {
        try {
            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(usuario.getLogin())
                    .withExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS))
                    .sign(algoritmo);
        } catch (JWTCreationException ex) {
            throw new IllegalStateException("Erro ao gerar token JWT", ex);
        }
    }

    public String getSubject(String tokenJWT) {
        try {
            var subject = JWT.require(algoritmo)
                    .withIssuer(ISSUER)
                    .withClaimPresence("exp")
                    .build()
                    .verify(tokenJWT)
                    .getSubject();
            if (subject == null || subject.isBlank()) {
                throw new BadCredentialsException("Token sem usuário");
            }
            return subject;
        } catch (JWTVerificationException | IllegalArgumentException | DateTimeException ex) {
            throw new BadCredentialsException("Token JWT inválido ou expirado", ex);
        }
    }
}
