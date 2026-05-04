package com.resumeai.aiservice.service;

import com.resumeai.aiservice.client.GeminiClient;
import com.resumeai.aiservice.config.AiProviderConfig;
import com.resumeai.aiservice.dto.AiRequestDTO;
import com.resumeai.aiservice.dto.AtsReportDTO;
import com.resumeai.aiservice.dto.QuotaDTO;
import com.resumeai.aiservice.entity.AiRequest;
import com.resumeai.aiservice.exception.AiProviderException;
import com.resumeai.aiservice.exception.QuotaExceededException;
import com.resumeai.aiservice.exception.ResourceNotFoundException;
import com.resumeai.aiservice.mapper.AiRequestMapper;
import com.resumeai.aiservice.repository.AiRequestRepository;
import com.resumeai.aiservice.AiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private AiRequestRepository aiRequestRepository;
    @Mock
    private AiRequestMapper aiRequestMapper;
    @Mock
    private GeminiClient geminiClient;
    @Mock
    private AiProviderConfig aiProviderConfig;

    @InjectMocks
    private AiServiceImpl aiService;

    private AiRequest mockAiRequest;
    private AiRequestDTO mockAiRequestDTO;

    @BeforeEach
    void setUp() {
        UUID requestId = UUID.randomUUID();
        mockAiRequest = AiRequest.builder()
                .requestId(requestId)
                .userId(1L)
                .build();

        mockAiRequestDTO = AiRequestDTO.builder()
                .requestId(requestId)
                .aiResponse("Mocked AI Response")
                .build();

        // By default, assume the user has not hit their quota
        lenient().when(aiProviderConfig.getQuota()).thenReturn(null); // Defaults to 100 limit
        lenient().when(aiRequestRepository.countByUserIdAndCreatedAtAfter(eq(1L), any(LocalDateTime.class))).thenReturn(10);
    }

    @Test
    void testGenerateSummary_Success() throws Exception {
        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.callAiProvider(anyString())).thenReturn("Improved Summary");
        when(geminiClient.getModelName()).thenReturn("gemini-1.5-flash");
        when(aiRequestRepository.save(any(AiRequest.class))).thenReturn(mockAiRequest);
        when(aiRequestMapper.toDTO(any(AiRequest.class))).thenReturn(mockAiRequestDTO);

        AiRequestDTO response = aiService.generateSummary(1L, 10L, "Old Summary");

        assertNotNull(response);
        verify(geminiClient).callAiProvider(anyString());
        verify(aiRequestRepository).save(any(AiRequest.class));
    }

    @Test
    void testQuotaExceededException() {
        // Simulate used quota equals total quota (100)
        when(aiRequestRepository.countByUserIdAndCreatedAtAfter(eq(1L), any(LocalDateTime.class))).thenReturn(100);

        assertThrows(QuotaExceededException.class, () ->
                aiService.generateSummary(1L, 10L, "Old Summary")
        );
    }

    @Test
    void testGeminiUnavailableThrowsException() {
        when(geminiClient.isAvailable()).thenReturn(false);
        when(aiRequestRepository.save(any(AiRequest.class))).thenReturn(mockAiRequest);

        assertThrows(AiProviderException.class, () ->
                aiService.generateBullets(1L, 10L, "Job Experience")
        );

        // Verifies that a FAILED record was still saved to the DB
        verify(aiRequestRepository, times(1)).save(any(AiRequest.class));
    }

    @Test
    void testCheckAtsCompatibility_ValidJsonParse() throws Exception {
        when(geminiClient.isAvailable()).thenReturn(true);

        // Simulating the exact JSON return structure from the AI prompt
        String aiJsonResponse = "```json\n" +
                "{\n" +
                "  \"atsScore\": 85,\n" +
                "  \"matchedKeywords\": [\"Java\", \"Spring\"],\n" +
                "  \"missingKeywords\": [\"Docker\"],\n" +
                "  \"improvements\": [\"Add Docker\"],\n" +
                "  \"overallFeedback\": \"Good resume.\"\n" +
                "}\n```";

        when(geminiClient.callAiProvider(anyString())).thenReturn(aiJsonResponse);
        when(aiRequestRepository.save(any(AiRequest.class))).thenReturn(mockAiRequest);

        AiRequestDTO internalResponse = new AiRequestDTO();
        internalResponse.setAiResponse(aiJsonResponse);
        when(aiRequestMapper.toDTO(any(AiRequest.class))).thenReturn(internalResponse);

        AtsReportDTO report = aiService.checkAtsCompatibility(1L, 10L, "Resume using Java Spring", "Job wants Java Spring Docker");

        assertNotNull(report);
        assertEquals(85, report.getAtsScore());
        assertEquals(2, report.getMatchedKeywords().size());
        assertEquals(1, report.getMissingKeywords().size());
    }

    @Test
    void testCheckAtsCompatibility_HeuristicFallback() throws Exception {
        when(geminiClient.isAvailable()).thenReturn(true);

        // Simulating AI failing to return JSON and returning plain text instead
        String badJsonResponse = "Sorry, I can't generate JSON right now.";
        when(geminiClient.callAiProvider(anyString())).thenReturn(badJsonResponse);
        when(aiRequestRepository.save(any(AiRequest.class))).thenReturn(mockAiRequest);

        AiRequestDTO internalResponse = new AiRequestDTO();
        internalResponse.setAiResponse(badJsonResponse);
        when(aiRequestMapper.toDTO(any(AiRequest.class))).thenReturn(internalResponse);

        // Resume and JD to trigger heuristic fallback logic matching words > 3 length
        String resume = "Developed microservices";
        String jd = "Looking for someone who developed microservices with kubernetes";

        AtsReportDTO report = aiService.checkAtsCompatibility(1L, 10L, resume, jd);

        assertNotNull(report);
        assertTrue(report.getAtsScore() > 0); // Should calculate based on matching "developed" and "microservices"
        assertEquals(badJsonResponse, report.getOverallFeedback());
    }

    @Test
    void testGetRequestHistory_Success() {
        when(aiRequestRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(mockAiRequest));
        when(aiRequestMapper.toDTO(mockAiRequest)).thenReturn(mockAiRequestDTO);

        List<AiRequestDTO> history = aiService.getRequestHistory(1L);

        assertFalse(history.isEmpty());
        assertEquals(1, history.size());
    }

    @Test
    void testGetRequestHistory_NotFound() {
        when(aiRequestRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> aiService.getRequestHistory(1L));
    }

    @Test
    void testGetQuotaInfo() {
        QuotaDTO quotaInfo = aiService.getQuotaInfo(1L);

        assertEquals(1L, quotaInfo.getUserId());
        assertEquals(100, quotaInfo.getTotalMonthlyQuota());
        assertEquals(10, quotaInfo.getUsedQuota());
        assertEquals(90, quotaInfo.getRemainingQuota());
        assertEquals("FREE", quotaInfo.getTierType());
    }

    @Test
    void testTailorResumeForJob() throws Exception {
        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.callAiProvider(anyString())).thenReturn("{ \"resume\": \"data\" }");
        when(aiRequestRepository.save(any(AiRequest.class))).thenReturn(mockAiRequest);
        when(aiRequestMapper.toDTO(any(AiRequest.class))).thenReturn(mockAiRequestDTO);

        AiRequestDTO response = aiService.tailorResumeForJob(1L, 10L, "{}", "Job Description");

        assertNotNull(response);
        verify(geminiClient).callAiProvider(anyString());
    }

    @Test
    void testImproveResume() throws Exception {
        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.callAiProvider(anyString())).thenReturn("Improved section");
        when(aiRequestRepository.save(any(AiRequest.class))).thenReturn(mockAiRequest);
        when(aiRequestMapper.toDTO(any(AiRequest.class))).thenReturn(mockAiRequestDTO);

        AiRequestDTO response = aiService.improveResume(1L, 10L, "Old section");

        assertNotNull(response);
    }
}