package com.resumeai.export_service.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.html2pdf.HtmlConverter;
import com.resumeai.export_service.dto.TemplateExportRequest;
import com.resumeai.export_service.dto.TemplatePersonalInfo;
import com.resumeai.export_service.dto.TemplateResumeData;
import com.resumeai.export_service.dto.TemplateSectionData;
import com.resumeai.export_service.dto.TemplateStyleConfig;
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

        return generatePdf(buildStandardResumeHtml(resume));
    }

    public byte[] exportTemplatePdf(TemplateExportRequest request) {
        TemplateResumeData resumeData = request.getResumeData();
        if (resumeData == null || resumeData.getPersonalInfo() == null) {
            throw new IllegalArgumentException("Template resume data is required for export.");
        }

        return generatePdf(buildTemplateResumeHtml(request));
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

    private byte[] generatePdf(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            HtmlConverter.convertToPdf(html, outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate PDF export", ex);
        }
    }

    private String buildStandardResumeHtml(ResumeSnapshot resume) {
        try {
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

            appendLegacyExperienceSection(html, content.path("experience"));
            appendLegacyEducationSection(html, content.path("education"));
            appendLegacySkillsSection(html, content.path("skills"));

            html.append("</div></body></html>");
            return html.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build standard resume HTML", ex);
        }
    }

    private String buildTemplateResumeHtml(TemplateExportRequest request) {
        TemplateResumeData resume = request.getResumeData();
        TemplatePersonalInfo personal = resume.getPersonalInfo();
        String variant = normalizeVariant(request.getTemplateId(), request.getStyleConfig());

        StringBuilder html = new StringBuilder("""
                <html>
                  <head>
                    <meta charset="UTF-8" />
                    <style>
                      @page { size: A4; margin: 0; }
                      body {
                        margin: 0;
                        color: #0f172a;
                        background: #ffffff;
                      }
                      * {
                        box-sizing: border-box;
                      }
                      .page {
                        width: 210mm;
                        height: 297mm;
                        overflow: hidden;
                      }
                      .resume-layout {
                        min-height: 297mm;
                      }
                      h1, h2, h3, p, ul {
                        margin: 0;
                      }
                      .section {
                        page-break-inside: avoid;
                      }
                      .section + .section {
                        margin-top: 14px;
                      }
                      .section-title {
                        font-size: 11pt;
                        text-transform: uppercase;
                        letter-spacing: 1.4px;
                        margin-bottom: 8px;
                      }
                      .entry {
                        page-break-inside: avoid;
                        margin-bottom: 12px;
                      }
                      .entry-header {
                        width: 100%;
                        border-collapse: collapse;
                      }
                      .entry-title {
                        font-weight: 700;
                        font-size: 11.5pt;
                      }
                      .entry-meta {
                        text-align: right;
                        white-space: nowrap;
                        font-size: 9.5pt;
                      }
                      .entry-subtitle {
                        margin-top: 2px;
                        font-size: 10pt;
                      }
                      .body-copy {
                        font-size: 10pt;
                        line-height: 1.45;
                      }
                      .bullet-list {
                        margin-top: 6px;
                        padding-left: 16px;
                      }
                      .bullet-list li {
                        font-size: 10pt;
                        line-height: 1.45;
                        margin-bottom: 4px;
                      }
                      .contact-grid {
                        font-size: 9.5pt;
                      }
                      .contact-item {
                        display: block;
                        margin-bottom: 4px;
                      }
                      .tag {
                        display: inline-block;
                        padding: 3px 9px;
                        margin: 0 6px 6px 0;
                        font-size: 9pt;
                      }
                      a {
                        color: inherit;
                        text-decoration: none;
                      }
                """);

        html.append(buildTemplateCss(variant, request.getStyleConfig()));
        html.append("</style></head><body>");
        html.append("<div class=\"page ").append(variant).append("\">");
        html.append("<div class=\"resume-layout\">");

        switch (variant) {
            case "template-modern" -> appendModernTemplate(html, resume, personal);
            case "template-creative" -> appendCreativeTemplate(html, resume, personal);
            case "template-executive" -> appendExecutiveTemplate(html, resume, personal);
            case "template-minimalist" -> appendMinimalistTemplate(html, resume, personal);
            default -> appendProfessionalTemplate(html, resume, personal);
        }

        html.append("</div></div></body></html>");
        return html.toString();
    }

    private String buildTemplateCss(String variant, TemplateStyleConfig styleConfig) {
        String accent = safeColor(styleConfig != null ? styleConfig.getAccentColor() : null, defaultAccent(variant));
        return """
                .template-modern { font-family: Helvetica, Arial, sans-serif; }
                .template-modern .resume-layout { display: table; width: 100%%; }
                .template-modern .sidebar, .template-modern .main { display: table-cell; vertical-align: top; }
                .template-modern .sidebar { width: 70mm; background: #0f172a; color: #f8fafc; padding: 14mm 8mm; }
                .template-modern .main { padding: 14mm 14mm 12mm; }
                .template-modern .name { font-size: 25pt; font-weight: 700; color: #ffffff; }
                .template-modern .headline { margin-top: 6px; margin-bottom: 20px; color: #94a3b8; font-size: 11pt; }
                .template-modern .sidebar .section-title { color: %s; border-bottom: 1px solid #334155; padding-bottom: 4px; }
                .template-modern .main .section-title { color: #0f172a; border-bottom: 2px solid #e2e8f0; padding-bottom: 4px; }
                .template-modern .tag { background: rgba(56, 189, 248, 0.16); color: #e0f2fe; border-radius: 999px; }
                .template-modern .entry-meta, .template-modern .entry-subtitle { color: #475569; }

                .template-professional { font-family: Georgia, 'Times New Roman', serif; border-top: 8px solid #1e3a8a; padding: 14mm; }
                .template-professional .header { border-bottom: 2px solid #1e3a8a; padding-bottom: 6mm; margin-bottom: 6mm; }
                .template-professional .name { font-size: 26pt; color: #1e3a8a; }
                .template-professional .headline { font-size: 11pt; color: #475569; margin-top: 4px; }
                .template-professional .contact-row { margin-top: 12px; }
                .template-professional .contact-item { display: inline-block; margin-right: 12px; }
                .template-professional .section-title { color: #1e3a8a; border-bottom: 1px solid #bfdbfe; padding-bottom: 4px; }
                .template-professional .tag { background: #eff6ff; color: #1e3a8a; border-radius: 999px; }
                .template-professional .entry-meta, .template-professional .entry-subtitle { color: #475569; }

                .template-creative { font-family: Helvetica, Arial, sans-serif; }
                .template-creative .resume-layout { display: table; width: 100%%; }
                .template-creative .sidebar, .template-creative .main { display: table-cell; vertical-align: top; }
                .template-creative .sidebar { width: 65mm; background: #fdf4ff; border-right: 2px dashed #f0abfc; padding: 14mm 8mm; }
                .template-creative .main { padding: 14mm 14mm 12mm; }
                .template-creative .name { font-size: 28pt; font-weight: 700; color: #a21caf; }
                .template-creative .headline { font-size: 11pt; color: #d946ef; margin-top: 4px; margin-bottom: 18px; }
                .template-creative .section-title { color: #86198f; background: #fae8ff; display: inline-block; padding: 4px 10px; border-radius: 4px; }
                .template-creative .tag { background: #ffffff; color: #86198f; border: 1px solid #f0abfc; border-radius: 999px; }
                .template-creative .entry-meta, .template-creative .entry-subtitle { color: #6b7280; }

                .template-executive { font-family: 'Times New Roman', Times, serif; padding: 14mm 18mm; background: #fcfcfc; }
                .template-executive .header { text-align: center; margin-bottom: 7mm; }
                .template-executive .name { font-size: 30pt; text-transform: uppercase; letter-spacing: 2px; border-bottom: 1px solid #111827; padding-bottom: 4px; }
                .template-executive .headline { font-size: 10.5pt; font-style: italic; margin-top: 6px; }
                .template-executive .contact-row { margin-top: 10px; }
                .template-executive .contact-item { display: inline-block; margin: 0 8px 4px; }
                .template-executive .section-title { color: #334155; border-bottom: 2px solid #334155; padding-bottom: 3px; }
                .template-executive .tag { border-bottom: 1px dotted #94a3b8; padding: 2px 0; margin-right: 12px; }
                .template-executive .entry-meta, .template-executive .entry-subtitle { color: #475569; }

                .template-minimalist { font-family: Helvetica, Arial, sans-serif; padding: 15mm 20mm; }
                .template-minimalist .header { border-bottom: 1px solid #e2e8f0; padding-bottom: 6mm; margin-bottom: 6mm; text-align: center; }
                .template-minimalist .name { font-size: 27pt; font-weight: 700; }
                .template-minimalist .headline { margin-top: 4px; font-size: 11pt; color: #64748b; }
                .template-minimalist .contact-row { margin-top: 10px; }
                .template-minimalist .contact-item { display: inline-block; margin: 0 8px 4px; color: #475569; }
                .template-minimalist .section-title { color: #0f172a; border-bottom: 1px solid #e2e8f0; padding-bottom: 6px; }
                .template-minimalist .tag { background: #f8fafc; color: #334155; border: 1px solid #e2e8f0; border-radius: 999px; }
                .template-minimalist .entry-meta, .template-minimalist .entry-subtitle { color: #64748b; }
                """.formatted(accent);
    }

    private void appendModernTemplate(StringBuilder html, TemplateResumeData resume, TemplatePersonalInfo personal) {
        html.append("<div class=\"sidebar\">");
        appendHeaderBlock(html, personal, false);
        appendTagSection(html, "Skills", resume.getSkills());
        appendTextSection(html, "Certifications", resume.getCertifications());
        appendTextSection(html, "Languages", resume.getLanguages());
        html.append("</div><div class=\"main\">");
        appendSummarySection(html, resume.getSummary());
        appendStructuredSection(html, "Experience", resume.getExperience(), true);
        appendStructuredSection(html, "Education", resume.getEducation(), false);
        appendStructuredSection(html, "Projects", resume.getProjects(), false);
        html.append("</div>");
    }

    private void appendCreativeTemplate(StringBuilder html, TemplateResumeData resume, TemplatePersonalInfo personal) {
        html.append("<div class=\"sidebar\">");
        appendHeaderBlock(html, personal, false);
        appendTagSection(html, "Skills", resume.getSkills());
        appendTextSection(html, "Certifications", resume.getCertifications());
        appendTextSection(html, "Languages", resume.getLanguages());
        html.append("</div><div class=\"main\">");
        appendSummarySection(html, resume.getSummary());
        appendStructuredSection(html, "Experience", resume.getExperience(), true);
        appendStructuredSection(html, "Education", resume.getEducation(), false);
        appendStructuredSection(html, "Projects", resume.getProjects(), false);
        html.append("</div>");
    }

    private void appendExecutiveTemplate(StringBuilder html, TemplateResumeData resume, TemplatePersonalInfo personal) {
        html.append("<div class=\"content\">");
        appendHeaderBlock(html, personal, true);
        appendSummarySection(html, resume.getSummary());
        appendStructuredSection(html, "Experience", resume.getExperience(), true);
        appendStructuredSection(html, "Education", resume.getEducation(), false);
        appendTagSection(html, "Skills", resume.getSkills());
        appendStructuredSection(html, "Projects", resume.getProjects(), false);
        appendTextSection(html, "Certifications", resume.getCertifications());
        appendTextSection(html, "Languages", resume.getLanguages());
        html.append("</div>");
    }

    private void appendMinimalistTemplate(StringBuilder html, TemplateResumeData resume, TemplatePersonalInfo personal) {
        html.append("<div class=\"content\">");
        appendHeaderBlock(html, personal, true);
        appendSummarySection(html, resume.getSummary());
        appendStructuredSection(html, "Experience", resume.getExperience(), true);
        appendStructuredSection(html, "Education", resume.getEducation(), false);
        appendStructuredSection(html, "Projects", resume.getProjects(), false);
        appendTagSection(html, "Skills", resume.getSkills());
        appendTextSection(html, "Certifications", resume.getCertifications());
        appendTextSection(html, "Languages", resume.getLanguages());
        html.append("</div>");
    }

    private void appendProfessionalTemplate(StringBuilder html, TemplateResumeData resume, TemplatePersonalInfo personal) {
        html.append("<div class=\"content\">");
        appendHeaderBlock(html, personal, false);
        appendSummarySection(html, resume.getSummary());
        appendStructuredSection(html, "Experience", resume.getExperience(), true);
        appendStructuredSection(html, "Education", resume.getEducation(), false);
        appendTagSection(html, "Skills", resume.getSkills());
        appendStructuredSection(html, "Projects", resume.getProjects(), false);
        appendTextSection(html, "Certifications", resume.getCertifications());
        appendTextSection(html, "Languages", resume.getLanguages());
        html.append("</div>");
    }

    private void appendHeaderBlock(StringBuilder html, TemplatePersonalInfo personal, boolean centered) {
        html.append("<div class=\"header\">")
                .append("<h1 class=\"name\">").append(escape(orFallback(personal.getFullName(), "Your Name"))).append("</h1>");

        if (!isBlank(personal.getHeadline())) {
            html.append("<p class=\"headline\">").append(escape(personal.getHeadline())).append("</p>");
        }

        html.append("<div class=\"contact-grid");
        if (centered) {
            html.append(" contact-row");
        }
        html.append("\">");
        appendContactItem(html, personal.getEmail());
        appendContactItem(html, personal.getPhone());
        appendContactItem(html, personal.getLocation());
        appendContactItem(html, personal.getLinkedin());
        appendContactItem(html, personal.getGithub());
        appendContactItem(html, personal.getPortfolio());
        html.append("</div></div>");
    }

    private void appendSummarySection(StringBuilder html, String summary) {
        if (isBlank(summary)) {
            return;
        }

        html.append("<div class=\"section\">")
                .append("<h2 class=\"section-title\">Summary</h2>")
                .append("<p class=\"body-copy\">").append(escape(summary)).append("</p>")
                .append("</div>");
    }

    private void appendStructuredSection(StringBuilder html, String title, List<TemplateSectionData> items, boolean renderBullets) {
        List<TemplateSectionData> safeItems = items == null ? List.of() : items;
        safeItems = safeItems.stream().filter(item -> item != null && hasMeaningfulContent(item)).toList();
        if (safeItems.isEmpty()) {
            return;
        }

        html.append("<div class=\"section\">")
                .append("<h2 class=\"section-title\">").append(escape(title)).append("</h2>");

        for (TemplateSectionData item : safeItems) {
            html.append("<div class=\"entry\">")
                    .append("<table class=\"entry-header\"><tr>")
                    .append("<td class=\"entry-title\">").append(escape(orFallback(item.getTitle(), title))).append("</td>")
                    .append("<td class=\"entry-meta\">").append(escape(orFallback(item.getDateRange(), ""))).append("</td>")
                    .append("</tr></table>");

            if (!isBlank(item.getSubtitle())) {
                html.append("<p class=\"entry-subtitle\">").append(escape(item.getSubtitle())).append("</p>");
            }

            if (!isBlank(item.getDescription())) {
                html.append("<p class=\"body-copy\">").append(escape(item.getDescription())).append("</p>");
            }

            if (!isBlank(item.getLink())) {
                String safeLink = escapeAttribute(normalizeLink(item.getLink()));
                html.append("<p class=\"body-copy\"><a href=\"").append(safeLink).append("\">")
                        .append(escape(item.getLink())).append("</a></p>");
            }

            if (renderBullets) {
                List<String> bullets = sanitizeList(item.getBullets());
                if (!bullets.isEmpty()) {
                    html.append("<ul class=\"bullet-list\">");
                    for (String bullet : bullets) {
                        html.append("<li>").append(escape(stripBulletPrefix(bullet))).append("</li>");
                    }
                    html.append("</ul>");
                }
            }

            html.append("</div>");
        }

        html.append("</div>");
    }

    private void appendTagSection(StringBuilder html, String title, List<String> items) {
        List<String> safeItems = sanitizeList(items);
        if (safeItems.isEmpty()) {
            return;
        }

        html.append("<div class=\"section\">")
                .append("<h2 class=\"section-title\">").append(escape(title)).append("</h2>");
        for (String item : safeItems) {
            html.append("<span class=\"tag\">").append(escape(item)).append("</span>");
        }
        html.append("</div>");
    }

    private void appendTextSection(StringBuilder html, String title, List<String> items) {
        List<String> safeItems = sanitizeList(items);
        if (safeItems.isEmpty()) {
            return;
        }

        html.append("<div class=\"section\">")
                .append("<h2 class=\"section-title\">").append(escape(title)).append("</h2>");
        for (String item : safeItems) {
            html.append("<p class=\"body-copy\">").append(escape(item)).append("</p>");
        }
        html.append("</div>");
    }

    private void appendLegacyExperienceSection(StringBuilder html, JsonNode experience) {
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
                    String bullet = escape(stripBulletPrefix(responsibility.asText("")));
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

    private void appendLegacyEducationSection(StringBuilder html, JsonNode education) {
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

    private void appendLegacySkillsSection(StringBuilder html, JsonNode skills) {
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

    private void appendContactItem(StringBuilder html, String value) {
        if (!isBlank(value)) {
            html.append("<span class=\"contact-item\">").append(escape(value)).append("</span>");
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

    private String normalizeVariant(String templateId, TemplateStyleConfig styleConfig) {
        String variant = styleConfig != null && !isBlank(styleConfig.getVariant())
                ? styleConfig.getVariant()
                : templateId;

        String normalized = variant == null ? "professional" : variant.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("template-")) {
            normalized = normalized.substring("template-".length());
        }
        if ("minimal".equals(normalized)) {
            normalized = "minimalist";
        }
        return "template-" + normalized;
    }

    private String defaultAccent(String variant) {
        return switch (variant) {
            case "template-modern" -> "#38bdf8";
            case "template-creative" -> "#a21caf";
            case "template-executive" -> "#334155";
            case "template-minimalist" -> "#0f172a";
            default -> "#1e3a8a";
        };
    }

    private String safeColor(String candidate, String fallback) {
        if (candidate == null || !candidate.matches("^#?[0-9a-fA-F]{6}$")) {
            return fallback;
        }
        return candidate.startsWith("#") ? candidate : "#" + candidate;
    }

    private boolean hasMeaningfulContent(TemplateSectionData item) {
        return !isBlank(item.getTitle())
                || !isBlank(item.getSubtitle())
                || !isBlank(item.getDescription())
                || !isBlank(item.getDateRange())
                || !sanitizeList(item.getBullets()).isEmpty()
                || !isBlank(item.getLink());
    }

    private List<String> sanitizeList(List<String> values) {
        List<String> sanitized = new ArrayList<>();
        if (values == null) {
            return sanitized;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                sanitized.add(value.trim());
            }
        }
        return sanitized;
    }

    private String normalizeLink(String value) {
        if (isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private String orFallback(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String stripBulletPrefix(String value) {
        return value == null ? "" : value.replaceFirst("^[-*•\\s]+", "").trim();
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

    private String escapeAttribute(String value) {
        return escape(value).replace("<br/>", "");
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
