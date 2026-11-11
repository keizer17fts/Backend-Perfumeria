package cl.luxury.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Comprueba el CORS del gateway con la "pre-solicitud" (OPTIONS) que el navegador envía antes de un PATCH/POST/DELETE.
 * No necesita que los microservicios estén levantados: la pre-solicitud se responde en el gateway.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsTest {

    private static final String ORIGEN_VITE = "http://localhost:5173";

    @Autowired
    WebTestClient web;

    @Test
    void permiteElOrigenDeVitePorCadaMetodoQueUsaElFront() {
        for (String metodo : new String[]{"GET", "POST", "PUT", "PATCH", "DELETE"}) {
            web.method(HttpMethod.OPTIONS).uri("/api/ordenes")
                    .header("Origin", ORIGEN_VITE)
                    .header("Access-Control-Request-Method", metodo)
                    .header("Access-Control-Request-Headers", "content-type")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals("Access-Control-Allow-Origin", ORIGEN_VITE);
        }
    }

    @Test
    void cubreLasRutasDeLosTresMicroservicios() {
        for (String ruta : new String[]{"/api/productos", "/api/usuarios/1", "/api/auth/login", "/api/mensajes"}) {
            web.method(HttpMethod.OPTIONS).uri(ruta)
                    .header("Origin", ORIGEN_VITE)
                    .header("Access-Control-Request-Method", "POST")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().exists("Access-Control-Allow-Origin");
        }
    }

    @Test
    void rechazaUnOrigenQueNoEstaEnLaListaBlanca() {
        web.method(HttpMethod.OPTIONS).uri("/api/ordenes")
                .header("Origin", "http://sitio-malicioso.example")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isForbidden();
    }
}
