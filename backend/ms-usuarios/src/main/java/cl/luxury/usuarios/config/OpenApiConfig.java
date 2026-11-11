package cl.luxury.usuarios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI apiDocumentada() {
        return new OpenAPI()
            .info(new Info().title("Luxury Perfumería - API Usuarios")
                .version("1.0.0")
                .description("Gestión de cuentas, perfiles y autenticación.\n\nCRUD REST: GET consulta, POST crea (201), PUT reemplaza, PATCH actualiza parcialmente y DELETE elimina (204). " +
                             "Respuestas de error comunes: 400 solicitud inválida, 404 recurso inexistente y 409 conflicto. " +
                             "Las operaciones de pedidos, autenticación y alertas tienen reglas propias; consultar cada endpoint.")
                .contact(new Contact().name("Proyecto universitario Luxury Perfumería")))
            .addServersItem(new Server().url("http://localhost:8082").description("Microservicio local"))
            .addServersItem(new Server().url("http://localhost:8080").description("API Gateway"));
    }
}
