package com.resumeai.aiservice.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtsReportDTO {

	private Long userId;
	private Long resumeId;
	private Integer atsScore; // 0-100
	private List<String> matchedKeywords;
	private List<String> missingKeywords;
	private Map<String, Double> semanticScores; // Field -> similarity score
	private String overallFeedback;
	private List<String> improvements;
	private Integer totalKeywordsChecked;
	private Integer keywordsMatched;
}

