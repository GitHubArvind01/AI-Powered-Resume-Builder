package com.resumeai.export_service.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.html2pdf.HtmlConverter;
import com.resumeai.export_service.exception.ResourceNotFoundException;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumePdfExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    private final DiscoveryClient discoveryClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public byte[] exportResumePdf(Long resumeId, Long userId) {
        ResumeSnapshot resume = fetchResume(resumeId);

        if (resume.getUserId() == null || !resume.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Resume not found for the authenticated user: " + resumeId);
        }

        return generatePdf(resume);
    }

    private ResumeSnapshot fetchResume(Long resumeId) {
        List<ServiceInstance> instances = discoveryClient.getInstances("RESUME-SERVICE");
        if (instances.isEmpty()) {
            throw new IllegalStateException("Resume service is unavailable");
        }

        String endpoint = instances.get(0).getUri() + "/api/v1/resumes/" + resumeId;
        log.info("Fetching resume {} from {}", resumeId, endpoint);

        ResponseEntity<ResumeSnapshot> response = restTemplate.getForEntity(endpoint, ResumeSnapshot.class);
        ResumeSnapshot body = response.getBody();
        if (body == null) {
            throw new ResourceNotFoundException("Resume not found: " + resumeId);
        }

        return body;
    }

    private byte[] generatePdf(ResumeSnapshot resume) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            String html = buildResumeHtml(resume);
            HtmlConverter.convertToPdf(html, outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate PDF export", ex);
        }
    }

    private String buildResumeHtml(ResumeSnapshot resume) throws Exception {
        JsonNode content = parseContent(resume.getContent());
        JsonNode personalInfo = content.path("personalInfo");
        String fullName = escape(textValue(personalInfo, "fullName", resume.getTitle(), "Your Name"));
        String email = escape(textValue(personalInfo, "email", null, ""));
        String phone = escape(textValue(personalInfo, "phone", null, ""));
        String location = escape(textValue(personalInfo, "location", null, ""));
        String summary = escape(textValue(content, "summary", resume.getDescription(), ""));

        StringBuilder html = new StringBuilder("""
                <html>
                  <head>
                    <meta charset="UTF-8" />
                    <style>
                      @page { size: A4; margin: 28px; }
                      body {
                        font-family: Helvetica, Arial, sans-serif;
                        color: #1f2937;
                        margin: 0;
                        font-size: 11pt;
                        line-height: 1.5;
                      }
                      .page {
                        border-top: 6px solid #1d4ed8;
                        padding-top: 20px;
                      }
                      .header {
                        margin-bottom: 18px;
                      }
                      .name {
                        font-size: 24pt;
                        font-weight: 700;
                        color: #0f172a;
                        margin: 0 0 4px;
                      }
                      .contact {
                        color: #475569;
                        font-size: 10pt;
                      }
                      .contact span {
                        margin-right: 12px;
                      }
                      .section {
                        margin-top: 18px;
                      }
                      .section-title {
                        font-size: 12pt;
                        font-weight: 700;
                        letter-spacing: 0.04em;
                        text-transform: uppercase;
                        color: #1d4ed8;
                        margin: 0 0 8px;
                        padding-bottom: 4px;
                        border-bottom: 1px solid #cbd5e1;
                      }
                      .entry {
                        margin-bottom: 14px;
                      }
                      .entry-header {
                        width: 100%;
                        border-collapse: collapse;
                      }
                      .entry-title {
                        font-weight: 700;
                        color: #0f172a;
                      }
                      .entry-meta {
                        text-align: right;
                        color: #475569;
                        white-space: nowrap;
                      }
                      .entry-subtitle {
                        color: #334155;
                        margin: 2px 0 6px;
                      }
                      ul {
                        margin: 6px 0 0 18px;
                        padding: 0;
                      }
                      li {
                        margin-bottom: 4px;
                      }
                      .skills-list {
                        margin: 0;
                        padding-left: 18px;
                      }
                      .muted {
                        color: #64748b;
                      }
                    </style>
                  </head>
                  <body>
                    <div class="page">
                """);

        html.append("<div class=\"header\">")
                .append("<h1 class=\"name\">").append(fullName).append("</h1>")
                .append("<div class=\"contact\">");

        appendContact(html, email);
        appendContact(html, phone);
        appendContact(html, location);
        html.append("</div></div>");

        if (!summary.isBlank()) {
            html.append("<section class=\"section\">")
                    .append("<h2 class=\"section-title\">Summary</h2>")
                    .append("<p>").append(summary).append("</p>")
                    .append("</section>");
        }

        appendExperienceSection(html, content.path("experience"));
        appendEducationSection(html, content.path("education"));
        appendSkillsSection(html, content.path("skills"));

        html.append("</div></body></html>");
        return html.toString();
    }

    private void appendExperienceSection(StringBuilder html, JsonNode experience) {
        if (!experience.isArray() || experience.isEmpty()) {
            return;
        }

        html.append("<section class=\"section\"><h2 class=\"section-title\">Experience</h2>");
        for (JsonNode entry : experience) {
            String jobTitle = escape(textValue(entry, "jobTitle", null, "Professional Experience"));
            String company = escape(textValue(entry, "companyName", null, ""));
            String startDate = formatDate(textValue(entry, "startDate", null, ""));
            String endDate = formatDate(textValue(entry, "endDate", null, "Present"));

            html.append("<div class=\"entry\">")
                    .append("<table class=\"entry-header\"><tr>")
                    .append("<td class=\"entry-title\">").append(jobTitle).append("</td>")
                    .append("<td class=\"entry-meta\">").append(escape(startDate))
                    .append(startDate.isBlank() && endDate.isBlank() ? "" : " - ")
                    .append(escape(endDate)).append("</td>")
                    .append("</tr></table>");

            if (!company.isBlank()) {
                html.append("<p class=\"entry-subtitle\">").append(company).append("</p>");
            }

            JsonNode responsibilities = entry.path("responsibilities");
            if (responsibilities.isArray() && !responsibilities.isEmpty()) {
                html.append("<ul>");
                for (JsonNode responsibility : responsibilities) {
                    String bullet = escape(responsibility.asText("").replaceFirst("^[-*•\\s]+", "").trim());
                    if (!bullet.isBlank()) {
                        html.append("<li>").append(bullet).append("</li>");
                    }
                }
                html.append("</ul>");
            }

            html.append("</div>");
        }
        html.append("</section>");
    }

    private void appendEducationSection(StringBuilder html, JsonNode education) {
        if (!education.isArray() || education.isEmpty()) {
            return;
        }

        html.append("<section class=\"section\"><h2 class=\"section-title\">Education</h2>");
        for (JsonNode entry : education) {
            String degree = escape(textValue(entry, "degree", null, "Education"));
            String school = escape(textValue(entry, "school", null, ""));
            String graduationDate = escape(formatDate(textValue(entry, "graduationDate", null, "")));

            html.append("<div class=\"entry\">")
                    .append("<table class=\"entry-header\"><tr>")
                    .append("<td class=\"entry-title\">").append(degree).append("</td>")
                    .append("<td class=\"entry-meta\">").append(graduationDate).append("</td>")
                    .append("</tr></table>");

            if (!school.isBlank()) {
                html.append("<p class=\"entry-subtitle\">").append(school).append("</p>");
            }

            html.append("</div>");
        }
        html.append("</section>");
    }

    private void appendSkillsSection(StringBuilder html, JsonNode skills) {
        if (!skills.isArray() || skills.isEmpty()) {
            return;
        }

        html.append("<section class=\"section\"><h2 class=\"section-title\">Skills</h2><ul class=\"skills-list\">");
        for (JsonNode skill : skills) {
            String value = "";
            if (skill.isTextual()) {
                value = skill.asText("");
            } else if (skill.hasNonNull("skill")) {
                value = skill.get("skill").asText("");
            }

            value = escape(value.trim());
            if (!value.isBlank()) {
                html.append("<li>").append(value).append("</li>");
            }
        }
        html.append("</ul></section>");
    }

    private void appendContact(StringBuilder html, String value) {
        if (!value.isBlank()) {
            html.append("<span>").append(value).append("</span>");
        }
    }

    private JsonNode parseContent(String content) throws Exception {
        if (content == null || content.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(content);
    }

    private String textValue(JsonNode node, String fieldName, String fallback, String defaultValue) {
        if (node != null && node.hasNonNull(fieldName)) {
            return node.get(fieldName).asText(defaultValue);
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return defaultValue;
    }

    private String formatDate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if ("Present".equalsIgnoreCase(value.trim())) {
            return "Present";
        }
        try {
            return DATE_FORMATTER.format(java.time.YearMonth.parse(value.trim()));
        } catch (Exception ignored) {
            return value;
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
        return escaped.replace("\n", "<br/>");
    }

    @Data
    public static class ResumeSnapshot {
        private Long id;
        private Long userId;
        private String title;
        private String content;
        private String status;
        private String description;
    }
}
