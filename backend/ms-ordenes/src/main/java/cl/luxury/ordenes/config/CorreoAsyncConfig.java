package cl.luxury.ordenes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Envíos SMTP en segundo plano, con capacidad limitada para no saturar el servidor.
 * El commit de la orden ocurre antes de agendar el correo.
 */
@Configuration
public class CorreoAsyncConfig {
    @Bean(name = "correoExecutor")
    public ThreadPoolTaskExecutor correoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("luxury-correo-");
        // No usar CallerRunsPolicy: ante saturación bloquearía otra vez el checkout.
        executor.initialize();
        return executor;
    }
}
