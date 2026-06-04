package capstone2.team3.realplan.domain.ai.service;

import capstone2.team3.realplan.domain.ai.client.AiClient;
import capstone2.team3.realplan.domain.ai.dto.AiPlanningErrorRateRequest;
import capstone2.team3.realplan.domain.ai.dto.AiPlanningErrorRateResponse;
import capstone2.team3.realplan.domain.ai.entity.AiCoefficientUpdateLog;
import capstone2.team3.realplan.domain.ai.entity.AiSystemPrior;
import capstone2.team3.realplan.domain.ai.entity.UserAiDifficultyResidual;
import capstone2.team3.realplan.domain.ai.entity.UserAiFolderResidual;
import capstone2.team3.realplan.domain.ai.entity.UserAiProfile;
import capstone2.team3.realplan.domain.ai.entity.UserAiTypeResidual;
import capstone2.team3.realplan.domain.ai.repository.AiCoefficientUpdateLogRepository;
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
public class TaskAiCoefficientUpdateService {

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
    private final AiCoefficientUpdateLogRepository aiCoefficientUpdateLogRepository;
    private final AiSystemPriorRepository aiSystemPriorRepository;
    private final UserAiProfileRepository userAiProfileRepository;
    private final UserAiTypeResidualRepository userAiTypeResidualRepository;
    private final UserAiDifficultyResidualRepository userAiDifficultyResidualRepository;
    private final UserAiFolderResidualRepository userAiFolderResidualRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void updateOnTaskCompleted(Task task) {
        int estimatedMinutes = resolveEstimatedMinutes(task);
        int actualMinutes = task.getTotalTime();
        if (estimatedMinutes <= 0 || actualMinutes <= 0) {
            return;
        }

        AiPlanningErrorRateRequest request = buildRequest(task, estimatedMinutes, actualMinutes);
        try {
            AiPlanningErrorRateResponse response = aiClient.updatePlanningErrorRates(request);
            if (!response.dropped()) {
                applyCoefficientUpdate(task, request, response);
            }
            saveUpdateLog(task, request, response);
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.AI_SERVICE_UNAVAILABLE) {
                throw e;
            }
            log.warn("AI coefficient update skipped. taskId={}, reason={}", task.getTaskId(), e.getErrorCode().getCode());
        } catch (RuntimeException e) {
            log.warn("AI coefficient update skipped. taskId={}", task.getTaskId(), e);
        }
    }

    private AiPlanningErrorRateRequest buildRequest(Task task, int estimatedMinutes, int actualMinutes) {
        Long userId = task.getUser().getUserId();
        UserAiProfile profile = userAiProfileRepository.findByUserUserId(userId).orElse(null);
        AiSystemPrior systemPrior = aiSystemPriorRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc().orElse(null);

        return new AiPlanningErrorRateRequest(
                estimatedMinutes,
                actualMinutes,
                profile != null ? profile.getCompletedCount() : 0,
                task.getTaskType().getCode(),
                task.getDifficulty().name(),
                task.getFolder().getFolderId().toString(),
                profile != null ? profile.getUserGlobal().doubleValue() : null,
                userTypeResidualMap(userId),
                userDifficultyResidualMap(userId),
                userFolderResidualMap(userId),
                typeCountMap(userId),
                difficultyCountMap(userId),
                folderCountMap(userId),
                systemPrior != null ? systemPrior.getSystemGlobalPrior().doubleValue() : 0.0,
                systemPrior != null
                        ? parseEffectMap(systemPrior.getSystemTypeEffect(), DEFAULT_SYSTEM_TYPE_EFFECT)
                        : DEFAULT_SYSTEM_TYPE_EFFECT,
                systemPrior != null
                        ? parseEffectMap(systemPrior.getSystemDifficultyEffect(), DEFAULT_SYSTEM_DIFFICULTY_EFFECT)
                        : DEFAULT_SYSTEM_DIFFICULTY_EFFECT
        );
    }

    private void applyCoefficientUpdate(
            Task task,
            AiPlanningErrorRateRequest request,
            AiPlanningErrorRateResponse response
    ) {
        Long userId = task.getUser().getUserId();
        UserAiProfile profile = userAiProfileRepository.findByUserUserId(userId)
                .orElseGet(() -> userAiProfileRepository.save(UserAiProfile.builder()
                        .user(task.getUser())
                        .build()));
        profile.update(response.userGlobal(), request.completedCount() + 1);

        String taskTypeCode = task.getTaskType().getCode();
        UserAiTypeResidual typeResidual = userAiTypeResidualRepository
                .findByUserUserIdAndTaskTypeTaskTypeId(userId, task.getTaskType().getTaskTypeId())
                .orElseGet(() -> userAiTypeResidualRepository.save(UserAiTypeResidual.builder()
                        .user(task.getUser())
                        .taskType(task.getTaskType())
                        .build()));
        typeResidual.update(
                response.userTypeResidual().getOrDefault(taskTypeCode, typeResidual.getResidual().doubleValue()),
                response.typeCount().getOrDefault(taskTypeCode, typeResidual.getSampleCount())
        );

        String difficulty = task.getDifficulty().name();
        UserAiDifficultyResidual difficultyResidual = userAiDifficultyResidualRepository
                .findByUserUserIdAndDifficulty(userId, task.getDifficulty())
                .orElseGet(() -> userAiDifficultyResidualRepository.save(UserAiDifficultyResidual.builder()
                        .user(task.getUser())
                        .difficulty(task.getDifficulty())
                        .build()));
        difficultyResidual.update(
                response.userDifficultyResidual().getOrDefault(difficulty, difficultyResidual.getResidual().doubleValue()),
                response.difficultyCount().getOrDefault(difficulty, difficultyResidual.getSampleCount())
        );

        String folderId = task.getFolder().getFolderId().toString();
        UserAiFolderResidual folderResidual = userAiFolderResidualRepository
                .findByUserUserIdAndFolderFolderId(userId, task.getFolder().getFolderId())
                .orElseGet(() -> userAiFolderResidualRepository.save(UserAiFolderResidual.builder()
                        .user(task.getUser())
                        .folder(task.getFolder())
                        .build()));
        folderResidual.update(
                response.userFolderResidual().getOrDefault(folderId, folderResidual.getResidual().doubleValue()),
                response.folderCount().getOrDefault(folderId, folderResidual.getSampleCount())
        );
    }

    private void saveUpdateLog(
            Task task,
            AiPlanningErrorRateRequest request,
            AiPlanningErrorRateResponse response
    ) {
        aiCoefficientUpdateLogRepository.save(AiCoefficientUpdateLog.builder()
                .task(task)
                .user(task.getUser())
                .estimatedMinutes(request.estimatedMinutes())
                .actualMinutes(request.actualMinutes())
                .planningErrorRatio(toDecimal(response.planningErrorRatio()))
                .clampedPlanningErrorRatio(toDecimal(response.clampedPlanningErrorRatio()))
                .logRatio(toDecimal(response.logRatio()))
                .clampedLogRatio(toDecimal(response.clampedLogRatio()))
                .stage(response.stage())
                .dropped(response.dropped())
                .dropReason(response.dropReason())
                .beforeSnapshot(toJson(request))
                .afterSnapshot(toJson(response))
                .build());
    }

    private int resolveEstimatedMinutes(Task task) {
        if (task.getUserEstimated() != null && task.getUserEstimated() > 0) {
            return task.getUserEstimated();
        }
        if (task.getFinalEstimated() != null && task.getFinalEstimated() > 0) {
            return task.getFinalEstimated();
        }
        if (task.getAiEstimated() != null && task.getAiEstimated() > 0) {
            return task.getAiEstimated();
        }
        return 0;
    }

    private Map<String, Double> userTypeResidualMap(Long userId) {
        return userAiTypeResidualRepository.findAllByUserUserId(userId).stream()
                .collect(Collectors.toMap(
                        residual -> residual.getTaskType().getCode(),
                        residual -> residual.getResidual().doubleValue()
                ));
    }

    private Map<String, Integer> typeCountMap(Long userId) {
        return userAiTypeResidualRepository.findAllByUserUserId(userId).stream()
                .collect(Collectors.toMap(
                        residual -> residual.getTaskType().getCode(),
                        UserAiTypeResidual::getSampleCount
                ));
    }

    private Map<String, Double> userDifficultyResidualMap(Long userId) {
        return userAiDifficultyResidualRepository.findAllByUserUserId(userId).stream()
                .collect(Collectors.toMap(
                        residual -> residual.getDifficulty().name(),
                        residual -> residual.getResidual().doubleValue()
                ));
    }

    private Map<String, Integer> difficultyCountMap(Long userId) {
        return userAiDifficultyResidualRepository.findAllByUserUserId(userId).stream()
                .collect(Collectors.toMap(
                        residual -> residual.getDifficulty().name(),
                        UserAiDifficultyResidual::getSampleCount
                ));
    }

    private Map<String, Double> userFolderResidualMap(Long userId) {
        return userAiFolderResidualRepository.findAllByUserUserId(userId).stream()
                .collect(Collectors.toMap(
                        residual -> residual.getFolder().getFolderId().toString(),
                        residual -> residual.getResidual().doubleValue()
                ));
    }

    private Map<String, Integer> folderCountMap(Long userId) {
        return userAiFolderResidualRepository.findAllByUserUserId(userId).stream()
                .collect(Collectors.toMap(
                        residual -> residual.getFolder().getFolderId().toString(),
                        UserAiFolderResidual::getSampleCount
                ));
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

    private BigDecimal toDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
