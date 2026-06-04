package capstone2.team3.realplan.domain.ai.client;

import capstone2.team3.realplan.domain.ai.dto.*;

public interface AiClient {

    AiHealthResponse health();

    AiTaskClassifyResponse classifyTask(AiTaskClassifyRequest request);

    AiTaskEstimateResponse estimateTask(AiTaskEstimateRequest request);

    AiSessionEstimateResponse estimateSession(AiSessionEstimateRequest request);

    AiPlanningErrorRateResponse updatePlanningErrorRates(AiPlanningErrorRateRequest request);

    AiTaskRecommendResponse recommendTasks(AiTaskRecommendRequest request);

    AiTaskDecomposeResponse decomposeTasks(AiTaskDecomposeRequest request);

    AiScheduleAutoPlaceResponse autoPlaceSchedule(AiScheduleAutoPlaceRequest request);
}
