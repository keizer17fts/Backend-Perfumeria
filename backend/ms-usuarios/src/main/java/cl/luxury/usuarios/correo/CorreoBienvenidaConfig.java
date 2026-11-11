package cl.luxury.usuarios.correo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class CorreoBienvenidaConfig {
    @Bean(name = "bienvenidaExecutor")
    public ThreadPoolTaskExecutor bienvenidaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("bienvenida-correo-");
        executor.initialize();
        return executor;
    }
}
