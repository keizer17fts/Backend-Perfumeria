package cl.luxury.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Configuration
public class Seguridad {
    private static final String BASE = "/api/v1/";
    private final SecretKeySpec clave;
    private final JwtEncoder encoder;

    public Seguridad(@Value("${MAISON_JWT_SECRET}") String secreto) {
        byte[] bytes = Base64.getDecoder().decode(secreto);
        if (bytes.length < 32) throw new IllegalArgumentException("La clave JWT requiere al menos 32 bytes");
        clave = new SecretKeySpec(bytes, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    public String emitir(String sujeto, String rol) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("maison").subject(sujeto)
            .issuedAt(ahora).expiresAt(ahora.plus(8, ChronoUnit.HOURS)).claim("rol", rol).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Bean
    JwtDecoder decoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("maison"));
        return decoder;
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("rol");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter jwt = new JwtAuthenticationConverter();
        jwt.setJwtGrantedAuthoritiesConverter(roles);
        return http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**","/error").permitAll()
                .requestMatchers(HttpMethod.GET, BASE+"productos/**", BASE+"categorias/**", BASE+"ofertas/**",
                    BASE+"resenas", BASE+"temporada", BASE+"cupones/*/validar", BASE+"*/salud").permitAll()
                .requestMatchers(HttpMethod.POST, BASE+"auth/login", BASE+"auth/google").permitAll()
                .requestMatchers(HttpMethod.POST, BASE+"usuarios", BASE+"mensajes", BASE+"alertas-stock/*", BASE+"ordenes")
                    .access((auth, ctx) -> new org.springframework.security.authorization.AuthorizationDecision(
                        auth.get().getAuthorities().stream().noneMatch(r -> r.getAuthority().equals("ROLE_Vendedor"))))
                .requestMatchers(HttpMethod.POST, BASE+"inventario/**").hasAnyRole("Inventario","Administrador")
                .requestMatchers(HttpMethod.POST, BASE+"resenas").hasAnyRole("Cliente","Administrador")
                .requestMatchers(HttpMethod.GET, BASE+"ordenes").hasAnyRole("Cliente","Administrador","Vendedor","Resenas")
                .requestMatchers(HttpMethod.GET, BASE+"ordenes/**").authenticated()
                .requestMatchers(HttpMethod.POST, BASE+"ordenes/*/boleta/correo").hasAnyRole("Cliente","Administrador","Invitado")
                .requestMatchers(HttpMethod.GET, BASE+"usuarios/*").hasAnyRole("Cliente","Administrador")
                .requestMatchers(HttpMethod.PATCH, BASE+"usuarios/*").hasAnyRole("Cliente","Administrador")
                .requestMatchers(HttpMethod.POST, BASE+"usuarios/*/cambiar-password").hasAnyRole("Cliente","Administrador")
                .anyRequest().hasRole("Administrador"))
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwt))).build();
    }

    public static boolean rol(String nombre) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_"+nombre));
    }

    public static String sujeto() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "" : auth.getName();
    }

    public static void exigir(boolean permitido) {
        if (!permitido) throw new ResponseStatusException(FORBIDDEN, "Sin permiso para esta operación");
    }
}
