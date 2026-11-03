package org.example.ac1devops;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.example.ac1devops.progression.api.StudentProgressionApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Executa o {@code main} de verdade, como no {@code java -jar}: servidor em porta livre e banco proprio,
 * para nao interferir nos contextos dos demais testes. O hook so captura o contexto, para poder fecha-lo.
 */
class Ac1DevOpsApplicationMainTest {

    @Test
    void mainDeveSubirAAplicacaoCompleta() {
        AtomicReference<ConfigurableApplicationContext> started = new AtomicReference<>();
        SpringApplicationRunListener listener = new SpringApplicationRunListener() {
            @Override
            public void ready(ConfigurableApplicationContext context, Duration timeTaken) {
                started.set(context);
            }
        };

        SpringApplication.withHook(application -> listener, () -> Ac1DevOpsApplication.main(new String[] {
            "--server.port=0",
            "--spring.datasource.url=jdbc:h2:mem:teste-main"
        }));

        try (ConfigurableApplicationContext context = started.get()) {
            assertThat(context.isRunning()).isTrue();
            assertThat(context.getBean(StudentProgressionApi.class).listStudents()).isEmpty();
        }
    }
}
