package cl.luxury.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto único de entrada: React llama solo a http://localhost:8080/api y el gateway reenvía cada ruta
 * al microservicio que corresponde (ver application.yml). Aquí también se configura CORS.
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
