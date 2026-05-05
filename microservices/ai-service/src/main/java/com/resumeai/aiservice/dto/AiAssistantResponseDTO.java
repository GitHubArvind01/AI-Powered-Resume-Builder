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
public class AiAssistantResponseDTO {
	private String content;
	private Integer remainingUsage;
	private boolean limitReached;
	private List<String> suggestions;
}
