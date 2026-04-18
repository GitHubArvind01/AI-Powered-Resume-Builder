# Resume Section Service - Production-Ready Microservice

## Overview
The Resume Section Service manages individual sections within resumes (SUMMARY, EXPERIENCE, EDUCATION, SKILLS, etc.). It provides comprehensive section management with reordering, visibility control, and AI-generation tracking through a well-architected REST API.

---

## Architecture & Layers

### 1. **Entity Layer** (`entity/`)
- **ResumeSection.java**: JPA entity mapping to the `resume_sections` table
  - **Fields:**
    - `sectionId`: Long (Primary Key, Auto-increment)
    - `resumeId`: Long (Foreign key reference to Resume Service)
    - `sectionType`: String (SUMMARY, EXPERIENCE, EDUCATION, SKILLS, CERTIFICATIONS, PROJECTS, LANGUAGES, VOLUNTEER, CUSTOM)
    - `title`: String (Max 255 characters)
    - `content`: String (Rich text JSON, up to 50KB)
    - `displayOrder`: Integer (For sorting)
    - `isVisible`: Boolean (Default: true)
    - `aiGenerated`: Boolean (Default: false)
    - `createdAt`: LocalDateTime (@CreationTimestamp)
    - `updatedAt`: LocalDateTime (@UpdateTimestamp)

### 2. **DTO Layer** (`dto/`)
- **ResumeSectionRequestDTO**: For POST/PUT operations
  - Validation: @NotNull for resumeId, @NotBlank for sectionType
  - Pattern validation for sectionType enum
  - Size constraints on title and content

- **ResumeSectionResponseDTO**: For API responses
  - All readable fields including timestamps
  - Safe for client consumption

### 3. **Mapper** (`mapper/`)
- **SectionMapper**: Entity-DTO conversions
  - Manual mapping for control
  - List conversion support

### 4. **Exception Handling** (`exception/`)
- **ResourceNotFoundException**: Section not found (404)
- **SectionServiceException**: Business errors (400)
- **ErrorResponse**: Standardized error JSON
- **GlobalExceptionHandler**: Centralized error handling

### 5. **Repository** (`repository/`)
- **SectionRepository**: JpaRepository with custom finders
  - `findByResumeId()`: All sections for resume
  - `findByResumeIdAndSectionType()`: Filter by type
  - `findBySectionId()`: Find by ID
  - `findByResumeIdOrderByDisplayOrder()`: Ordered retrieval
  - `findByAiGenerated()`: AI sections
  - `countByResumeId()`: Count sections
  - `deleteByResumeId()`: Delete all for resume
  - `deleteBySectionId()`: Delete specific section
  - `findByResumeIdAndIsVisible()`: Visible sections only
  - `findByResumeIdAndAiGenerated()`: AI sections for resume

### 6. **Service Layer** (`service/`)
- **SectionService (Interface)**: Business contract
- **SectionServiceImpl (Implementation)**: Core operations
  - **CRUD:**
    - `addSection()`: Create new section
    - `getSectionById()`: Retrieve by ID
    - `getSectionsByResume()`: All sections for resume
    - `updateSection()`: Update section
    - `deleteSection()`: Delete section

  - **Advanced:**
    - `getSectionsByResumeOrderByDisplayOrder()`: Ordered sections
    - `getSectionsByType()`: Filter by type
    - `deleteAllSectionsByResume()`: Bulk delete
    - `toggleVisibility()`: Show/hide section
    - `reorderSections()`: Reorder multiple sections
    - `bulkUpdateSections()`: Update multiple at once
    - `countSectionsByResume()`: Count stats
    - `getAiGeneratedSections()`: AI sections only

### 7. **Controller** (`controller/`)
- **SectionResource**: RESTful API
  - **Base Path:** `/api/v1/sections`
  - **Endpoints:**

    | Method | Path | Description | Status |
    |--------|------|-------------|--------|
    | POST | `/` | Add section | 201 |
    | GET | `/{id}` | Get section by ID | 200 |
    | GET | `/resume/{resumeId}` | Get all for resume | 200 |
    | GET | `/type` | Filter by type | 200 |
    | PUT | `/{id}` | Update section | 200 |
    | DELETE | `/{id}` | Delete section | 204 |
    | DELETE | `/resume/{resumeId}` | Delete all for resume | 204 |
    | PATCH | `/{id}/visibility` | Toggle visibility | 200 |
    | PATCH | `/reorder` | Reorder sections | 204 |
    | PUT | `/bulk` | Bulk update | 200 |

  - **Full Swagger/OpenAPI documentation**
  - **Success/error codes: 200, 201, 204, 400, 404, 500**

---

## Configuration

### `application.yml`
```yaml
spring:
  application:
    name: resume-section-service
  datasource:
    url: jdbc:h2:mem:resumesectiondb  # In-memory H2
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.H2Dialect
  jackson:
    serialization:
      write-dates-as-timestamps: false

server:
  port: 8082

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

---

## Testing

### Unit Tests (`SectionServiceImplTest.java`)
- **Framework:** JUnit 5 + Mockito
- **20+ test scenarios** covering:
  - Success paths (create, read, update, delete)
  - Exception scenarios (not found, validation)
  - Business logic (reorder, toggle visibility)
  - List operations

---

## API Endpoints Detail

### Create Section
```bash
POST /api/v1/sections
Content-Type: application/json

{
  "resumeId": 1,
  "sectionType": "EXPERIENCE",
  "title": "Senior Software Engineer",
  "content": "{...rich text...}",
  "displayOrder": 2,
  "isVisible": true,
  "aiGenerated": false
}
```

### Reorder Sections
```bash
PATCH /api/v1/sections/reorder?resumeId=1
Content-Type: application/json

[3, 1, 2, 4]  // New section order
```

### Toggle Visibility
```bash
PATCH /api/v1/sections/{sectionId}/visibility
```

---

## Section Types
- `SUMMARY`: Professional summary
- `EXPERIENCE`: Work experience
- `EDUCATION`: Educational background
- `SKILLS`: Technical/professional skills
- `CERTIFICATIONS`: Certifications and licenses
- `PROJECTS`: Portfolio projects
- `LANGUAGES`: Languages spoken
- `VOLUNTEER`: Volunteer experience
- `CUSTOM`: Custom sections

---

## Database Schema

### `resume_sections` Table
```sql
CREATE TABLE resume_sections (
  section_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resume_id BIGINT NOT NULL,
  section_type VARCHAR(50) NOT NULL,
  title VARCHAR(255) NOT NULL,
  content LONGTEXT,
  display_order INT,
  is_visible BOOLEAN DEFAULT true,
  ai_generated BOOLEAN DEFAULT false,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_resume_id (resume_id),
  INDEX idx_section_type (section_type)
);
```

---

## Best Practices

1. **Separation of Concerns**: Clean layering
2. **Security**: DTOs prevent entity exposure
3. **Error Handling**: Centralized exception handler
4. **Validation**: Comprehensive input validation
5. **Testing**: 20+ unit tests
6. **Documentation**: Full Swagger/OpenAPI
7. **Logging**: SLF4J throughout

---

## Running the Service

```bash
mvn clean package
mvn spring-boot:run
```

### Access Swagger UI
```
http://localhost:8082/swagger-ui.html
```

---

## Contact & Support
Contact the Resume AI Team for issues or questions.

