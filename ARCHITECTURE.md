# AI-Powered Resume Builder - Microservices Architecture

## Overview

The AI-Powered Resume Builder is a microservices-based application that allows users to create, manage, and build professional resumes with AI assistance. The architecture follows a service-oriented design with clear separation of concerns.

## Service Architecture

### 1. **Auth-Service** (Port: 8081)
**Responsibility:** User authentication and authorization

**Key Features:**
- User registration and login
- JWT token generation and validation
- OAuth2 integration (Google)
- Email verification
- Password reset

**Endpoints:**
- `POST /auth/register` - User registration
- `POST /auth/login` - User login
- `POST /auth/refresh` - Token refresh
- `POST /auth/logout` - User logout
- `POST /auth/google/login` - Google OAuth login

**Security:** Spring Security with JWT tokens

---

### 2. **Resume-Service** (Port: 8082)
**Responsibility:** Top-level resume entity management

**Key Features:**
- Resume creation, retrieval, update, deletion
- Resume duplication for different job applications
- Publish/unpublish resumes to public gallery
- View count tracking for analytics
- Resume filtering by user and public status

**Entity Hierarchy:**
```
User (from Auth-Service)
  └── Resume (managed by Resume-Service)
       └── Resume Sections (managed by ResumeSection-Service)
```

**Key Fields in Resume Entity:**
- `id` - Resume unique identifier
- `userId` - Links resume to specific user (Foreign Key to User)
- `title` - Resume title
- `content` - Overall resume content
- `isPublic` - Publication status
- `viewCount` - Analytics tracking
- `status` - Draft, Final, etc.
- `createdAt`, `updatedAt` - Timestamps

**Endpoints:**
- `GET /api/v1/resumes/welcome` - Health check
- `POST /api/v1/resumes` - Create resume
- `GET /api/v1/resumes/{id}` - Get resume by ID
- `GET /api/v1/resumes/user/{userId}` - Get all resumes for a user
- `PUT /api/v1/resumes/{id}` - Update resume
- `DELETE /api/v1/resumes/{id}` - Delete resume
- `POST /api/v1/resumes/{id}/duplicate` - Duplicate resume
- `PUT /api/v1/resumes/{id}/publish` - Publish/unpublish
- `GET /api/v1/resumes/public/all` - Get public resumes
- `GET /api/v1/resumes/user/{userId}/count` - Count user's resumes

---

### 3. **Resume-Section-Service** (Port: 8083)
**Responsibility:** Managing structured sections within resumes

**Key Features:**
- Section type management (SUMMARY, EXPERIENCE, EDUCATION, SKILLS, CERTIFICATIONS, PROJECTS, LANGUAGES, VOLUNTEER, CUSTOM)
- Drag-and-drop reordering with `displayOrder`
- Section visibility control
- AI-generated content tracking
- Bulk updates for efficient editing

**Entity Hierarchy:**
```
Resume (from Resume-Service)
  └── Resume Sections (managed by ResumeSection-Service)
```

**Key Fields in ResumeSection Entity:**
- `sectionId` - Section unique identifier
- `resumeId` - Links section to specific resume (Foreign Key)
- `sectionType` - Type of section (enum)
- `title` - Section title
- `content` - Section content (supports rich text)
- `displayOrder` - Order for UI rendering
- `isVisible` - Visibility flag
- `aiGenerated` - Marks AI-authored content
- `createdAt`, `updatedAt` - Timestamps

**Endpoints (Hierarchical REST Design):**
- `POST /api/v1/resumes/{resumeId}/sections` - Add section to resume
- `GET /api/v1/resumes/{resumeId}/sections` - Get all sections for resume
- `GET /api/v1/resumes/{resumeId}/sections/{sectionId}` - Get specific section
- `GET /api/v1/resumes/{resumeId}/sections/type/{type}` - Get sections by type
- `PUT /api/v1/resumes/{resumeId}/sections/{sectionId}` - Update section
- `DELETE /api/v1/resumes/{resumeId}/sections/{sectionId}` - Delete section
- `DELETE /api/v1/resumes/{resumeId}/sections` - Delete all sections for resume
- `PATCH /api/v1/resumes/{resumeId}/sections/{sectionId}/visibility` - Toggle visibility
- `PATCH /api/v1/resumes/{resumeId}/sections/reorder` - Reorder sections
- `PUT /api/v1/resumes/{resumeId}/sections/bulk` - Bulk update sections

**Endpoint Design:**
- Uses **nested routing** to reflect hierarchical relationship
- Path: `/api/v1/resumes/{resumeId}/sections` clearly indicates sections belong to resumes
- All operations are scoped to a specific resume

---

### 4. **Admin-Server** (Port: 9090)
**Responsibility:** Spring Boot Admin monitoring and management

**Key Features:**
- Real-time application monitoring
- Logs and metrics visualization
- Health status monitoring
- Configuration management

---

### 5. **API-Gateway** (Port: 8080)
**Responsibility:** Entry point for all client requests

**Key Features:**
- Request routing to microservices
- Load balancing
- Request/response transformation
- API versioning

---

### 6. **Eureka-Server** (Port: 8761)
**Responsibility:** Service discovery and registration

**Key Features:**
- Dynamic service registration
- Service discovery
- Health checking
- Load balancing support

---

## Data Flow Diagram

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌──────────────────┐
│  API Gateway     │
│  (Port 8080)     │
└──────┬───────────┘
       │
       ├──────────────────────────┐
       │                          │
       ▼                          ▼
┌─────────────────┐        ┌──────────────────┐
│  Auth-Service   │        │ Resume-Service   │
│  (Port 8081)    │        │  (Port 8082)     │
└─────────────────┘        └────────┬─────────┘
                                    │
                                    │ calls
                                    ▼
                          ┌──────────────────────────┐
                          │ Resume-Section-Service   │
                          │      (Port 8083)         │
                          └──────────────────────────┘
```

---

## Inter-Service Communication

### Resume-Service → Resume-Section-Service
**When:** Resume-Service operations need to manage associated sections
- Get sections for a resume
- Delete all sections when resume is deleted
- Duplicate sections when resume is duplicated

**Implementation:** To be implemented using:
- FeignClient for synchronous REST calls
- Message Queue (RabbitMQ) for asynchronous events

### Example Future Usage:
```java
@FeignClient(name = "resume-section-service")
public interface ResumeSectionClient {
    @GetMapping("/api/v1/resumes/{resumeId}/sections")
    List<ResumeSectionResponseDTO> getSectionsByResume(@PathVariable Long resumeId);
    
    @DeleteMapping("/api/v1/resumes/{resumeId}/sections")
    void deleteAllSectionsByResume(@PathVariable Long resumeId);
}
```

---

## User Context Flow

### Request Flow with User Context:
1. **Client** sends request with JWT token in Authorization header
2. **API-Gateway** validates and forwards to appropriate service
3. **Auth-Service** validates JWT if needed
4. **Resume-Service** extracts userId from JWT token context
5. **Resume-Service** ensures user can only access their own resumes
6. **Resume-Section-Service** validates sections belong to specified resume

### Example:
```
GET /api/v1/resumes/user/123/sections
└─ Service validates userId from JWT matches path parameter
└─ Service retrieves resumes for userId=123
└─ For each resume, retrieves sections via Resume-Section-Service
```

---

## Security Architecture

### Current Development Configuration:
- **Auth-Service**: Permits /auth/**, Swagger, H2-console
- **Resume-Service**: No Spring Security (public access)
- **Resume-Section-Service**: Permits /api/v1/**, Swagger, H2-console
- **CSRF**: Disabled for development and internal service communication

### Future Production Configuration:
- OAuth2/JWT validation on all service boundaries
- Service-to-service authentication
- Role-based access control (RBAC)
- Rate limiting and throttling

---

## Development Guidelines

### Adding a New Endpoint:
1. **Identify the appropriate service** based on responsibility
2. **Use hierarchical routing** - `/api/v1/resumes/{resumeId}/sections` format
3. **Include proper Swagger/OpenAPI annotations** for documentation
4. **Add validation** using Jakarta validation annotations
5. **Ensure DTOs include foreign keys** (userId, resumeId) for relationships
6. **Update Security Config** if new public endpoints are added

### Example: New AI Service
```
AI-Service (Port 8084)
├── Responsibility: AI-powered resume content generation
├── Endpoints: POST /api/v1/sections/{sectionId}/generate-content
├── Calls: Resume-Section-Service to fetch section data
├── Updates: Resume-Section-Service with AI-generated content
└── Events: Publishes resume updated event
```

---

## Database Schema Relationships

```sql
-- Users table (Auth-Service)
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    ...
);

-- Resumes table (Resume-Service)
CREATE TABLE resumes (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    ...
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Resume Sections table (Resume-Section-Service)
CREATE TABLE resume_sections (
    section_id BIGINT PRIMARY KEY,
    resume_id BIGINT NOT NULL,
    section_type VARCHAR(50),
    display_order INT,
    ...
    FOREIGN KEY (resume_id) REFERENCES resumes(id)
);
```

---

## Testing Strategy

### Unit Testing:
- Service layer logic
- DTO transformations
- Business rule validations

### Integration Testing:
- Controller endpoint testing
- Database interactions
- Exception handling

### End-to-End Testing:
- Complete user workflows
- Multi-service interactions
- API Gateway routing

---

## Deployment Checklist

- [ ] All services registered with Eureka
- [ ] API-Gateway routes configured for all services
- [ ] Security configurations validated
- [ ] Swagger UI accessible on all service ports
- [ ] Cross-service communication tested
- [ ] Database migrations completed
- [ ] Admin-Server monitoring verified
- [ ] Health checks passing

---

## Future Enhancements

1. **AI-Service** - AI-powered content generation and optimization
2. **Template-Service** - Resume template management
3. **PDF-Export-Service** - Resume export to PDF
4. **Notification-Service** - Email and push notifications
5. **Analytics-Service** - Resume analytics and insights
6. **Search-Service** - Elasticsearch integration for public resume search
7. **Cache-Service** - Redis caching layer for performance


