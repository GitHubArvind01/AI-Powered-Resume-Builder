package com.resumeai.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {

    private String to;
    private String otp;
    private String message;
    private String subject;
    private String purpose;
}
