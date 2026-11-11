package cl.luxury.ordenes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@org.springframework.context.annotation.Import(cl.luxury.security.Seguridad.class)
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class OrdenesApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrdenesApplication.class, args);
    }
}
