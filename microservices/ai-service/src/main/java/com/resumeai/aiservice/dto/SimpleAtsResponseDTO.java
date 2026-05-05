package com.resumeai.aiservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleAtsResponseDTO {
    private Integer score;
    private Integer keywordMatchPercentage;
    private List<String> keywordsMatched;
    private List<String> missingKeywords;
    private List<String> suggestions;
}
