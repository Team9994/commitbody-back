package team9499.commitbody.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@OpenAPIDefinition(
        info = @Info(title = "커밋 바디 API",
        description = "커밋 바디 스웨거_v1",
        version = "v1")
)
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openApi(){

        String socialToken ="SocialToken";

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(socialToken);

        SecurityScheme socialTokenSc = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("Bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name(HttpHeaders.AUTHORIZATION);

        Components components = new Components()
                .addSecuritySchemes(socialToken, socialTokenSc);

        return new OpenAPI()
                .addSecurityItem(securityRequirement)
                .components(components);
    }

}
