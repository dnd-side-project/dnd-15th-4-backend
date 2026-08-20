package com.dnd.puzzlemeet.global.config;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExample;
import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExamples;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class SwaggerConfig {

  private static final Set<String> NULL_TYPE = Set.of("null");

  @Bean
  public OpenAPI puzzleMeetOpenAPI() {
    return new OpenAPI()
        .info(new Info().title("PuzzleMeet API").description("퍼즐밋 백엔드 API 문서").version("v1"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }

  @Bean
  public OperationCustomizer customize() {
    return (Operation operation, HandlerMethod handlerMethod) -> {
      ApiErrorCodeExamples multi = handlerMethod.getMethodAnnotation(ApiErrorCodeExamples.class);
      if (multi != null) {
        generateErrorCodeResponseExample(operation, multi.value());
        return operation;
      }

      ApiErrorCodeExample single = handlerMethod.getMethodAnnotation(ApiErrorCodeExample.class);
      if (single != null) {
        generateErrorCodeResponseExample(operation, new ErrorCode[] {single.value()});
      }

      return operation;
    };
  }

  @Bean
  public OpenApiCustomizer nullableObjectSchemaCustomizer() {
    return openApi -> {
      Components components = openApi.getComponents();
      if (components == null || components.getSchemas() == null) {
        return;
      }
      components.getSchemas().values().forEach(this::restoreNullableObjectType);
    };
  }

  private void restoreNullableObjectType(Schema<?> schema) {
    if (schema == null) {
      return;
    }

    if (schema.get$ref() != null) {
      if (NULL_TYPE.equals(schema.getTypes())) {
        schema.setTypes(new LinkedHashSet<>(List.of("object", "null")));
      }
      return;
    }

    if (schema.getProperties() != null) {
      schema.getProperties().values().forEach(this::restoreNullableObjectType);
    }
    restoreNullableObjectType(schema.getItems());
    if (schema.getAdditionalProperties() instanceof Schema<?> additionalProperties) {
      restoreNullableObjectType(additionalProperties);
    }
  }

  private void generateErrorCodeResponseExample(Operation operation, ErrorCode[] errorCodes) {
    ApiResponses responses = operation.getResponses();
    if (responses == null) {
      responses = new ApiResponses();
      operation.setResponses(responses);
    }

    Map<HttpStatus, List<ErrorCode>> groupedByStatus =
        Arrays.stream(errorCodes)
            .collect(
                Collectors.groupingBy(
                    ErrorCode::getHttpStatus, LinkedHashMap::new, Collectors.toList()));

    ApiResponses target = responses;
    groupedByStatus.forEach(
        (httpStatus, codes) ->
            target.addApiResponse(
                String.valueOf(httpStatus.value()),
                new ApiResponse()
                    .description(httpStatus.getReasonPhrase())
                    .content(
                        new Content().addMediaType(APPLICATION_JSON_VALUE, toMediaType(codes)))));
  }

  private MediaType toMediaType(List<ErrorCode> errorCodes) {
    MediaType mediaType = new MediaType();
    errorCodes.forEach(
        errorCode -> mediaType.addExamples(errorCode.getCode(), toExample(errorCode)));
    return mediaType;
  }

  private Example toExample(ErrorCode errorCode) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", errorCode.getHttpStatus().value());
    body.put("code", errorCode.getCode());
    body.put("message", errorCode.getMessage());
    body.put("data", null);
    return new Example().summary(errorCode.getMessage()).value(body);
  }
}
