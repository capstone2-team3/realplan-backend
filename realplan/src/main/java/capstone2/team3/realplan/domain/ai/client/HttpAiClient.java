package capstone2.team3.realplan.domain.ai.client;

import capstone2.team3.realplan.domain.ai.dto.*;
import capstone2.team3.realplan.global.exception.BusinessException;
import capstone2.team3.realplan.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
public class HttpAiClient implements AiClient {

    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public HttpAiClient(@Value("${ai.base-url:http://localhost:8000}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public AiHealthResponse health() {
        return get("/health", AiHealthResponse.class);
    }

    @Override
    public AiTaskClassifyResponse classifyTask(AiTaskClassifyRequest request) {
        return post("/tasks/classify", request, AiTaskClassifyResponse.class);
    }

    @Override
    public AiTaskEstimateResponse estimateTask(AiTaskEstimateRequest request) {
        log.warn("AI estimate request body: {}", toJson(request));
        return post("/tasks/estimate", request, AiTaskEstimateResponse.class);
    }

    @Override
    public AiSessionEstimateResponse estimateSession(AiSessionEstimateRequest request) {
        return post("/sessions/estimate", request, AiSessionEstimateResponse.class);
    }

    @Override
    public AiPlanningErrorRateResponse updatePlanningErrorRates(AiPlanningErrorRateRequest request) {
        return post("/users/planning-error-rates", request, AiPlanningErrorRateResponse.class);
    }

    @Override
    public AiTaskRecommendResponse recommendTasks(AiTaskRecommendRequest request) {
        return post("/tasks/recommend", request, AiTaskRecommendResponse.class);
    }

    @Override
    public AiTaskDecomposeResponse decomposeTasks(AiTaskDecomposeRequest request) {
        return post("/tasks/decompose", request, AiTaskDecomposeResponse.class);
    }

    @Override
    public AiScheduleAutoPlaceResponse autoPlaceSchedule(AiScheduleAutoPlaceRequest request) {
        return post("/schedules/auto-place", request, AiScheduleAutoPlaceResponse.class);
    }

    private <T> T get(String path, Class<T> responseType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return extractData(path, response.statusCode(), response.body(), responseType);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("AI GET request failed. path={}", path, e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private <T> T post(String path, Object requestBody, Class<T> responseType) {
        String json = toJson(requestBody);
        log.warn("AI POST request. path={}, body={}", path, json);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return extractData(path, response.statusCode(), response.body(), responseType);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("AI POST request failed. path={}", path, e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private String toJson(Object request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize AI request. requestType={}", request.getClass().getSimpleName(), e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private <T> T extractData(String path, int statusCode, String body, Class<T> responseType) {
        log.warn("AI response. path={}, status={}, body={}", path, statusCode, body);
        if (statusCode < 200 || statusCode >= 300) {
            log.warn("AI request failed. path={}, status={}, body={}", path, statusCode, body);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            if (!"SUCCESS".equals(root.path("resultType").asText())) {
                log.warn("AI response failed. path={}, body={}", path, body);
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            JsonNode data = root.path("success").path("data");
            if (data.isMissingNode() || data.isNull()) {
                log.warn("AI success response has no data. path={}, body={}", path, body);
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            return objectMapper.treeToValue(data, responseType);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse AI response. path={}, body={}", path, body, e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }
}
