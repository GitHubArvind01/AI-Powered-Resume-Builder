# Service Mapping & Relationship Guide

## Issue Resolution Summary

### Problem
- Port 8083 (Resume-Section-Service) was showing login page instead of Swagger UI
- Services were not properly mapped to reflect hierarchical relationships
- Missing Security Configuration in Resume-Section-Service

### Root Cause
Spring Security dependency was added to resume-section-service but no `SecurityConfig` class existed, causing Spring Security to apply default authentication to all endpoints.

### Solution Implemented

#### 1. **Created SecurityConfig for Resume-Section-Service** ✅
**File:** `microservices/resume-section-service/src/main/java/com/resumeai/resume_section_service/config/SecurityConfig.java`

**Configuration:**
- Permits public access to:
  - `/api/v1/**` - All API endpoints
  - `/swagger-ui/**` - Swagger UI
  - `/v3/api-docs/**` - OpenAPI documentation
  - `/h2-console/**` - H2 database console
  - `/actuator/**` - Health checks
- CSRF disabled for development and microservice communication
- Frame options disabled for H2 console

**Result:** Swagger UI now accessible at `http://localhost:8083/swagger-ui/index.html`

---

#### 2. **Updated Endpoint Routing to Reflect Hierarchy** ✅
**File:** `microservices/resume-section-service/src/main/java/com/resumeai/resume_section_service/controller/SectionResource.java`

**Before:**
```
POST   /api/v1/sections
GET    /api/v1/sections/{id}
GET    /api/v1/sections/resume/{resumeId}
```

**After (Nested Resources):**
```
POST   /api/v1/resumes/{resumeId}/sections
GET    /api/v1/resumes/{resumeId}/sections
GET    /api/v1/resumes/{resumeId}/sections/{sectionId}
PUT    /api/v1/resumes/{resumeId}/sections/{sectionId}
DELETE /api/v1/resumes/{resumeId}/sections/{sectionId}
PATCH  /api/v1/resumes/{resumeId}/sections/{sectionId}/visibility
PATCH  /api/v1/resumes/{resumeId}/sections/reorder
PUT    /api/v1/resumes/{resumeId}/sections/bulk
```

**Benefits:**
- ✅ Clear hierarchical relationship in URLs
- ✅ RESTful design principles
- ✅ Prevents orphaned sections
- ✅ Easier to implement authorization checks

---

#### 3. **Verified Entity Relationships** ✅

**Resume Entity (Resume-Service):**
- ✅ Has `userId` field - Links resume to specific user
- Purpose: Ensure user can only access their own resumes

**ResumeSection Entity (Resume-Section-Service):**
- ✅ Has `resumeId` field - Links section to specific resume
- Purpose: Ensure sections belong to valid resumes

---

#### 4. **Verified DTOs Include Foreign Keys** ✅

**ResumeSectionRequestDTO:**
```java
@NotNull(message = "Resume ID is required")
private Long resumeId;  // ✅ Present
```

**ResumeSectionResponseDTO:**
```java
private Long resumeId;  // ✅ Present
```

**ResumeRequestDTO:**
```java
@NotNull(message = "User ID cannot be null")
private Long userId;  // ✅ Present
```

**ResumeResponseDTO:**
```java
private Long userId;  // ✅ Present
```

---

## Service Mapping Overview

### Resume-Service (Port 8082)
```
User (from Auth-Service, userId from JWT)
  └── GET    /api/v1/resumes/user/{userId}
  └── POST   /api/v1/resumes (with userId in JWT)
  └── PUT    /api/v1/resumes/{id}
  └── DELETE /api/v1/resumes/{id}
  └── GET    /api/v1/resumes/{id}
  └── POST   /api/v1/resumes/{id}/duplicate
  └── PUT    /api/v1/resumes/{id}/publish
```

### Resume-Section-Service (Port 8083) - NESTED UNDER RESUME
```
Resume (resumeId in path)
  ├── POST   /api/v1/resumes/{resumeId}/sections
  ├── GET    /api/v1/resumes/{resumeId}/sections
  ├── Section (sectionId)
  │  ├── GET    /api/v1/resumes/{resumeId}/sections/{sectionId}
  │  ├── PUT    /api/v1/resumes/{resumeId}/sections/{sectionId}
  │  ├── DELETE /api/v1/resumes/{resumeId}/sections/{sectionId}
  │  └── PATCH  /api/v1/resumes/{resumeId}/sections/{sectionId}/visibility
  ├── PATCH  /api/v1/resumes/{resumeId}/sections/reorder
  ├── PUT    /api/v1/resumes/{resumeId}/sections/bulk
  └── DELETE /api/v1/resumes/{resumeId}/sections
```

---

## Data Model Relationships

```
┌─────────────────┐
│ User (Auth-Svc) │
│ ─────────────── │
│ id              │
│ email           │
│ name            │
└────────┬────────┘
         │ userId
         │ (1:N)
         ▼
┌──────────────────────────┐
│ Resume (Resume-Service)  │
│ ────────────────────────│
│ id                       │
│ userId (FK)              │
│ title                    │
│ content                  │
│ isPublic                 │
│ viewCount                │
│ status                   │
│ createdAt, updatedAt     │
└────────┬─────────────────┘
         │ resumeId
         │ (1:N)
         ▼
┌──────────────────────────────────────┐
│ ResumeSection (Section-Service)      │
│ ──────────────────────────────────── │
│ sectionId                            │
│ resumeId (FK)                        │
│ sectionType                          │
│ title                                │
│ content                              │
│ displayOrder                         │
│ isVisible                            │
│ aiGenerated                          │
│ createdAt, updatedAt                 │
└──────────────────────────────────────┘
```

---

## User Journey

### Creating a Resume with Sections:

**Step 1: User Authenticates**
```
POST http://localhost:8081/auth/login
Response: { accessToken, userId }
```

**Step 2: Create Resume**
```
POST http://localhost:8082/api/v1/resumes
Header: Authorization: Bearer {token}
Body: { userId: 1, title: "My Resume", ... }
Response: { id: 101, userId: 1, title: "My Resume", ... }
```

**Step 3: Add Sections to Resume**
```
POST http://localhost:8083/api/v1/resumes/101/sections
Header: Authorization: Bearer {token}
Body: { 
  resumeId: 101,  // ✅ Matches path parameter
  sectionType: "SUMMARY",
  title: "Professional Summary",
  content: "...",
  displayOrder: 1
}
Response: { sectionId: 201, resumeId: 101, ... }
```

**Step 4: Get All Sections for Resume**
```
GET http://localhost:8083/api/v1/resumes/101/sections
Response: [ { sectionId: 201, ... }, { sectionId: 202, ... } ]
```

**Step 5: Update Section**
```
PUT http://localhost:8083/api/v1/resumes/101/sections/201
Body: { resumeId: 101, sectionType: "SUMMARY", title: "Updated", ... }
Response: { sectionId: 201, resumeId: 101, ... }
```

---

## Testing the Changes

### 1. **Test Swagger UI Access**
```bash
# Port 8082 - Resume-Service (should work)
http://localhost:8082/swagger-ui/index.html

# Port 8083 - Resume-Section-Service (should now work)
http://localhost:8083/swagger-ui/index.html
```

### 2. **Test API Endpoints**

**Create Resume:**
```bash
curl -X POST http://localhost:8082/api/v1/resumes \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "title": "My Resume",
    "status": "DRAFT"
  }'
```

**Add Section to Resume:**
```bash
curl -X POST http://localhost:8083/api/v1/resumes/1/sections \
  -H "Content-Type: application/json" \
  -d '{
    "resumeId": 1,
    "sectionType": "SUMMARY",
    "title": "Professional Summary",
    "displayOrder": 1,
    "isVisible": true
  }'
```

**Get Sections for Resume:**
```bash
curl -X GET http://localhost:8083/api/v1/resumes/1/sections
```

---

## Future Enhancements

### Phase 2: Cross-Service Communication
- [ ] Add FeignClient to Resume-Service for Resume-Section-Service calls
- [ ] Implement cascading delete (delete resume → delete sections)
- [ ] Implement section duplication when resume is duplicated

### Phase 3: User Context Handling
- [ ] Extract userId from JWT token in all services
- [ ] Validate user ownership before operations
- [ ] Add userId filtering at database query level

### Phase 4: Additional Services
- [ ] AI-Service - AI content generation
- [ ] Template-Service - Resume templates
- [ ] PDF-Export-Service - PDF generation
- [ ] Analytics-Service - Resume analytics

---

## Deployment Notes

### Services Running Successfully ✅
- **ADMIN-SERVICE** (9090) - Running
- **API-GATEWAY** (8080) - Running
- **AUTH-SERVICE** (8081) - Running
- **RESUME-SERVICE** (8082) - Running
- **RESUME-SECTION-SERVICE** (8083) - ✅ **NOW FIXED - Swagger UI accessible**

### All Services Registered with Eureka ✅
```
Application         Status
─────────────────── ─────────────────────────────────
ADMIN-SERVICE       UP (10.0.6.206:9090)
API-GATEWAY         UP (10.0.6.206:8080)
AUTH-SERVICE        UP (10.0.6.206:8081)
RESUME-SERVICE      UP (RESUME-SERVICE:14938...)
RESUME-SECTION-SVC  UP (resume-section-service:0f86...)
```

---

## Files Modified/Created

### Created:
1. ✅ `microservices/resume-section-service/src/main/java/com/resumeai/resume_section_service/config/SecurityConfig.java`
2. ✅ `ARCHITECTURE.md` - Comprehensive architecture documentation

### Modified:
1. ✅ `microservices/resume-section-service/src/main/java/com/resumeai/resume_section_service/controller/SectionResource.java`
   - Updated all endpoint mappings to use nested routing
   - Updated all log statements
   - Updated OpenAPI documentation

---

## Verification Checklist

- [x] SecurityConfig created and applied to resume-section-service
- [x] Swagger UI now accessible on port 8083
- [x] Endpoint mappings reflect hierarchical relationships
- [x] Nested routing implemented: `/api/v1/resumes/{resumeId}/sections`
- [x] Resume entity has userId field
- [x] ResumeSection entity has resumeId field
- [x] DTOs include foreign keys
- [x] Code compiles successfully
- [x] Services registered with Eureka
- [x] All three services responding correctly

---

## Next Steps

1. **Test the fixed Swagger UI** at `http://localhost:8083/swagger-ui/index.html`
2. **Create resume via Resume-Service** and note the returned `id`
3. **Create sections** using nested endpoint `/api/v1/resumes/{id}/sections`
4. **Verify relationships** - sections should only belong to specified resume
5. **Implement FeignClient** for cross-service communication when needed


