package capstone2.team3.realplan.domain.ai.service;

import capstone2.team3.realplan.domain.ai.client.AiClient;
import capstone2.team3.realplan.domain.ai.dto.AiTaskEstimateRequest;
import capstone2.team3.realplan.domain.ai.dto.AiTaskEstimateResponse;
import capstone2.team3.realplan.domain.ai.entity.AiEstimationLog;
import capstone2.team3.realplan.domain.ai.entity.AiSystemPrior;
import capstone2.team3.realplan.domain.ai.entity.UserAiDifficultyResidual;
import capstone2.team3.realplan.domain.ai.entity.UserAiFolderResidual;
import capstone2.team3.realplan.domain.ai.entity.UserAiProfile;
import capstone2.team3.realplan.domain.ai.entity.UserAiTypeResidual;
import capstone2.team3.realplan.domain.ai.repository.AiEstimationLogRepository;
import capstone2.team3.realplan.domain.ai.repository.AiSystemPriorRepository;
import capstone2.team3.realplan.domain.ai.repository.UserAiDifficultyResidualRepository;
import capstone2.team3.realplan.domain.ai.repository.UserAiFolderResidualRepository;
import capstone2.team3.realplan.domain.ai.repository.UserAiProfileRepository;
import capstone2.team3.realplan.domain.ai.repository.UserAiTypeResidualRepository;
import capstone2.team3.realplan.domain.task.entity.Task;
import capstone2.team3.realplan.global.exception.BusinessException;
import capstone2.team3.realplan.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskAiEstimationService {

    private static final Map<String, Double> DEFAULT_SYSTEM_TYPE_EFFECT = Map.of(
            "TIME_BASED", 0.0,
            "QUANTITY_BASED", 0.0,
            "SATISFACTION_BASED", 0.0
    );
    private static final Map<String, Double> DEFAULT_SYSTEM_DIFFICULTY_EFFECT = Map.of(
            "LOW", 0.0,
            "MEDIUM", 0.0,
            "HIGH", 0.0,
            "UNKNOWN", 0.0
    );

    private final AiClient aiClient;
    private final AiEstimationLogRepository aiEstimationLogRepository;
    private final AiSystemPriorRepository aiSystemPriorRepository;
    private final UserAiProfileRepository userAiProfileRepository;
    private final UserAiTypeResidualRepository userAiTypeResidualRepository;
    private final UserAiDifficultyResidualRepository userAiDifficultyResidualRepository;
    private final UserAiFolderResidualRepository userAiFolderResidualRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void estimateAndApply(Task task) {
        if (task.getUserEstimated() == null || task.getUserEstimated() <= 0) {
            return;
        }

        AiTaskEstimateRequest request = buildRequest(task);
        try {
            AiTaskEstimateResponse response = aiClient.estimateTask(request);
            int aiEstimatedMinutes = Math.max(1, (int) Math.round(response.aiEstimatedMinutes()));
            task.updateAiEstimated(aiEstimatedMinutes);
            saveEstimationLog(task, request, response);
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.AI_SERVICE_UNAVAILABLE) {
                throw e;
            }
            log.warn("AI task estimation skipped. taskId={}, reason={}", task.getTaskId(), e.getErrorCode().getCode());
            task.updateAiEstimated(task.getUserEstimated());
        } catch (RuntimeException e) {
            log.warn("AI task estimation skipped. taskId={}", task.getTaskId(), e);
            task.updateAiEstimated(task.getUserEstimated());
        }
    }

    private AiTaskEstimateRequest buildRequest(Task task) {
        Long userId = task.getUser().getUserId();
        UserAiProfile profile = userAiProfileRepository.findByUserUserId(userId).orElse(null);
        AiSystemPrior systemPrior = aiSystemPriorRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc().orElse(null);

        Map<String, Double> userTypeResidual = userAiTypeResidualRepository.findAllByUserUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        residual -> residual.getTaskType().getCode(),
                        residual -> residual.getResidual().doubleValue()
                ));
        Map<String, Integer> typeCount = userAiTypeResidualRepository.findAllByUserUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        residual -> residual.getTaskType().getCode(),
                        UserAiTypeResidual::getSampleCount
                ));

        Map<String, Double> userDifficultyResidual = userAiDifficultyResidualRepository.findAllByUserUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        residual -> residual.getDifficulty().name(),
                        residual -> residual.getResidual().doubleValue()
                ));
        Map<String, Integer> difficultyCount = userAiDifficultyResidualRepository.findAllByUserUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        residual -> residual.getDifficulty().name(),
                        UserAiDifficultyResidual::getSampleCount
                ));

        Map<String, Double> userFolderResidual = userAiFolderResidualRepository.findAllByUserUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        residual -> residual.getFolder().getFolderId().toString(),
                        residual -> residual.getResidual().doubleValue()
                ));
        Map<String, Integer> folderCount = userAiFolderResidualRepository.findAllByUserUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        residual -> residual.getFolder().getFolderId().toString(),
                        UserAiFolderResidual::getSampleCount
                ));

        return new AiTaskEstimateRequest(
                task.getUserEstimated(),
                profile != null ? profile.getCompletedCount() : 0,
                task.getTaskType().getCode(),
                task.getDifficulty().name(),
                task.getFolder().getFolderId().toString(),
                profile != null ? profile.getUserGlobal().doubleValue() : null,
                userTypeResidual,
                userDifficultyResidual,
                userFolderResidual,
                typeCount,
                difficultyCount,
                folderCount,
                systemPrior != null ? systemPrior.getSystemGlobalPrior().doubleValue() : 0.0,
                systemPrior != null
                        ? parseEffectMap(systemPrior.getSystemTypeEffect(), DEFAULT_SYSTEM_TYPE_EFFECT)
                        : DEFAULT_SYSTEM_TYPE_EFFECT,
                systemPrior != null
                        ? parseEffectMap(systemPrior.getSystemDifficultyEffect(), DEFAULT_SYSTEM_DIFFICULTY_EFFECT)
                        : DEFAULT_SYSTEM_DIFFICULTY_EFFECT
        );
    }

    private void saveEstimationLog(Task task, AiTaskEstimateRequest request, AiTaskEstimateResponse response) {
        aiEstimationLogRepository.save(AiEstimationLog.builder()
                .task(task)
                .user(task.getUser())
                .estimatedMinutes(request.estimatedMinutes())
                .aiEstimatedMinutes(toDecimal(response.aiEstimatedMinutes(), 2))
                .correctionFactor(toDecimal(response.correctionFactor(), 6))
                .logCorrection(toDecimal(response.logCorrection(), 6))
                .stage(response.stage())
                .inputSnapshot(toJson(request))
                .build());
    }

    private Map<String, Double> parseEffectMap(String json, Map<String, Double> fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<HashMap<String, Double>>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse AI system prior effect map. json={}", json, e);
            return fallback;
        }
    }

    private BigDecimal toDecimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
