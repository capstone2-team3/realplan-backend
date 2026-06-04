package capstone2.team3.realplan.domain.ai.client;

import capstone2.team3.realplan.domain.ai.dto.*;
import capstone2.team3.realplan.global.exception.BusinessException;
import capstone2.team3.realplan.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
public class HttpAiClient implements AiClient {

    private final RestClient restClient;

    public HttpAiClient(
            RestClient.Builder restClientBuilder,
            @Value("${ai.base-url:http://localhost:8000}") String baseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public AiHealthResponse health() {
        return get("/health", new ParameterizedTypeReference<AiApiResponse<AiHealthResponse>>() {
        });
    }

    @Override
    public AiTaskClassifyResponse classifyTask(AiTaskClassifyRequest request) {
        return post("/tasks/classify", request,
                new ParameterizedTypeReference<AiApiResponse<AiTaskClassifyResponse>>() {
                });
    }

    @Override
    public AiTaskEstimateResponse estimateTask(AiTaskEstimateRequest request) {
        return post("/tasks/estimate", request,
                new ParameterizedTypeReference<AiApiResponse<AiTaskEstimateResponse>>() {
                });
    }

    @Override
    public AiSessionEstimateResponse estimateSession(AiSessionEstimateRequest request) {
        return post("/sessions/estimate", request,
                new ParameterizedTypeReference<AiApiResponse<AiSessionEstimateResponse>>() {
                });
    }

    @Override
    public AiPlanningErrorRateResponse updatePlanningErrorRates(AiPlanningErrorRateRequest request) {
        return post("/users/planning-error-rates", request,
                new ParameterizedTypeReference<AiApiResponse<AiPlanningErrorRateResponse>>() {
                });
    }

    @Override
    public AiTaskRecommendResponse recommendTasks(AiTaskRecommendRequest request) {
        return post("/tasks/recommend", request,
                new ParameterizedTypeReference<AiApiResponse<AiTaskRecommendResponse>>() {
                });
    }

    @Override
    public AiTaskDecomposeResponse decomposeTasks(AiTaskDecomposeRequest request) {
        return post("/tasks/decompose", request,
                new ParameterizedTypeReference<AiApiResponse<AiTaskDecomposeResponse>>() {
                });
    }

    @Override
    public AiScheduleAutoPlaceResponse autoPlaceSchedule(AiScheduleAutoPlaceRequest request) {
        return post("/schedules/auto-place", request,
                new ParameterizedTypeReference<AiApiResponse<AiScheduleAutoPlaceResponse>>() {
                });
    }

    private <T> T get(String path, ParameterizedTypeReference<AiApiResponse<T>> responseType) {
        try {
            AiApiResponse<T> response = restClient.get()
                    .uri(path)
                    .retrieve()
                    .body(responseType);
            return extractData(path, response);
        } catch (RestClientException e) {
            log.warn("AI GET request failed. path={}", path, e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private <T> T post(String path, Object request, ParameterizedTypeReference<AiApiResponse<T>> responseType) {
        try {
            AiApiResponse<T> response = restClient.post()
                    .uri(path)
                    .body(request)
                    .retrieve()
                    .body(responseType);
            return extractData(path, response);
        } catch (RestClientException e) {
            log.warn("AI POST request failed. path={}", path, e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private <T> T extractData(String path, AiApiResponse<T> response) {
        if (response == null) {
            log.warn("AI response is empty. path={}", path);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
        if (!"SUCCESS".equals(response.resultType())) {
            log.warn("AI response failed. path={}, code={}, message={}",
                    path,
                    response.error() != null ? response.error().code() : null,
                    response.error() != null ? response.error().message() : null);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
        if (response.success() == null || response.success().data() == null) {
            log.warn("AI success response has no data. path={}", path);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
        return response.success().data();
    }

    private record AiApiResponse<T>(
            String resultType,
            Success<T> success,
            ErrorBody error,
            Meta meta
    ) {
    }

    private record Success<T>(
            T data
    ) {
    }

    private record ErrorBody(
            String code,
            String message
    ) {
    }

    private record Meta(
            String timestamp,
            String path
    ) {
    }
}
