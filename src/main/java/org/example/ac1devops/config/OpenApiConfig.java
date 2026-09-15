package org.example.ac1devops.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Identificacao da API na documentacao gerada a partir dos Controllers. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI educationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Educação Continuada Gamificada - API")
                        .version("1.0.0")
                        .description("API do projeto AC1 do Grupo 10. "
                                + "Documentação dos endpoints de alunos, XP e níveis disponibilizados pela aplicação."));
    }
}
