# Resume Service

## Overview
The Resume Service is a fully functional Spring Boot microservice for managing resumes in the AI-Powered Resume Builder ecosystem. It includes complete CRUD operations, advanced features like duplication, publishing, and view count tracking, with comprehensive error handling and unit testing.

---

## Architecture & Components

### 1. **Entity Layer**
- **Resume.java** - JPA Entity mapped to `resumes` table
  - Fields: id, userId, title, content, isPublic, viewCount, createdAt, updatedAt, status, description
  - Uses Lombok for boilerplate reduction
  - Supports Hibernate auto-updates with H2 database

### 2. **Data Transfer Objects (DTOs)**
- **ResumeRequestDTO** - For create/update operations (excludes system fields)
- **ResumeResponseDTO** - For returning data to clients
- **Manual Mapper** - ResumeMapper class for entity ↔ DTO conversions

### 3. **Repository Layer**
- **ResumeRepository** - Spring Data JPA interface with custom finders:
  - `findByUserId()` - Get all resumes for a user
  - `findByIsPublicTrue()` - Get all public resumes
  - `findByUserIdAndIsPublic()` - Filter by user and visibility
  - `existsByIdAndUserId()` - Verify user ownership
  - `countByUserId()` - Resume count per user
  - `findByStatus()` - Filter by status

### 4. **Service Layer**
- **ResumeService** (Interface) - Contract for business logic
- **ResumeServiceImpl** - Complete implementation with:
  - ✅ **createResume** - Create new resume
  - ✅ **getResumeById** - Fetch single resume with 404 handling
  - ✅ **updateResume** - Update existing resume
  - ✅ **deleteResume** - Soft/hard delete
  - ✅ **duplicateResume** - Deep copy with "Copy of" prefix
  - ✅ **publishResume** - Toggle public/private status
  - ✅ **incrementViewCount** - Track resume views
  - ✅ **getResumesByUserId** - List user's resumes
  - ✅ **getPublicResumes** - Discover public resumes
  - ✅ **getResumesByUserIdAndPublic** - Filtered queries

### 5. **Exception Handling**
- **GlobalExceptionHandler** - @ControllerAdvice catches:
  - `ResourceNotFoundException` → 404 Not Found
  - `ResumeServiceException` → 500 Internal Server Error
  - `MethodArgumentNotValidException` → 400 Bad Request
  - Generic `Exception` → 500 Internal Server Error
- **ErrorResponse** - Consistent JSON error format with timestamp, status, message, details

### 6. **Controller Layer**
- **ResumeResource** - REST API with 9 endpoints:
  ```
  GET  /api/v1/resumes/welcome              - Health check
  POST /api/v1/resumes                      - Create resume
  GET  /api/v1/resumes/{id}                 - Get resume by ID
  PUT  /api/v1/resumes/{id}                 - Update resume
  DELETE /api/v1/resumes/{id}               - Delete resume
  POST /api/v1/resumes/{id}/duplicate       - Duplicate resume
  PUT  /api/v1/resumes/{id}/publish         - Publish/unpublish
  GET  /api/v1/resumes/user/{userId}        - List user's resumes
  GET  /api/v1/resumes/user/{userId}/count  - Count user's resumes
  GET  /api/v1/resumes/public/all           - Get public resumes
  GET  /api/v1/resumes/user/{userId}/filter - Filter by visibility
  ```
- Full Swagger/OpenAPI 3 documentation with @Operation and @ApiResponse annotations

---

## Dependencies (pom.xml)

### Core Framework
- Spring Boot 3.2.2
- Spring Cloud 2023.0.1
- Spring Data JPA
- Spring Validation

### Database
- H2 Database (In-memory, development/testing)

### API Documentation
- SpringDoc OpenAPI 2.3.0 (Swagger UI)

### Utilities
- Lombok (Boilerplate reduction)
- Jackson (JSON serialization with LocalDateTime formatting)

### Service Discovery
- Eureka Client (Netflix Service Discovery)

### Monitoring
- Spring Boot Actuator
- Spring Boot Admin Client 3.2.0

### Testing
- JUnit 5
- Mockito
- Spring Boot Test

---

## API Documentation

All endpoints are documented with Swagger/OpenAPI 3.0:
- **Base Path**: `/api/v1/resumes`
- **Access Swagger UI**: `http://localhost:8082/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8082/v3/api-docs`

### Example Request/Response

#### Create Resume
```bash
POST /api/v1/resumes
Content-Type: application/json

{
  "userId": 1,
  "title": "My Resume",
  "content": "Experienced Software Developer...",
  "isPublic": false,
  "status": "DRAFT",
  "description": "My professional resume"
}
```

Response (201 Created):
```json
{
  "id": 1,
  "userId": 1,
  "title": "My Resume",
  "content": "Experienced Software Developer...",
  "isPublic": false,
  "viewCount": 0,
  "createdAt": "2026-04-18T01:00:00",
  "updatedAt": "2026-04-18T01:00:00",
  "status": "DRAFT",
  "description": "My professional resume"
}
```

#### Error Response (404)
```json
{
  "timestamp": "2026-04-18 01:00:00",
  "status": 404,
  "message": "Resume not found with ID: 999",
  "details": "/api/v1/resumes/999"
}
```

---

## Running the Service

### Prerequisites
- Java 17+
- Maven 3.6+

### Start the Service
```bash
cd microservices/resume-service
mvn spring-boot:run
```

### Access Points
- **Application**: http://localhost:8082
- **Swagger UI**: http://localhost:8082/swagger-ui.html
- **H2 Console**: http://localhost:8082/h2-console
- **Health Check**: http://localhost:8082/actuator/health
- **Resume Service**: http://localhost:8082/api/v1/resumes/welcome

---

## File Structure
```
microservices/resume-service/
├── src/
│   ├── main/
│   │   ├── java/com/resumeai/resume_service/
│   │   │   ├── entity/
│   │   │   │   └── Resume.java
│   │   │   ├── dto/
│   │   │   │   ├── ResumeRequestDTO.java
│   │   │   │   └── ResumeResponseDTO.java
│   │   │   ├── mapper/
│   │   │   │   └── ResumeMapper.java
│   │   │   ├── repository/
│   │   │   │   └── ResumeRepository.java
│   │   │   ├── service/
│   │   │   │   ├── ResumeService.java
│   │   │   │   └── impl/
│   │   │   │       └── ResumeServiceImpl.java
│   │   │   ├── controller/
│   │   │   │   └── ResumeResource.java
│   │   │   ├── exception/
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── ResumeServiceException.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── ResumeServiceApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/resumeai/resume_service/
│           ├── service/impl/
│           │   └── ResumeServiceImplTest.java
│           └── ResumeServiceApplicationTests.java
└── pom.xml
```

---

