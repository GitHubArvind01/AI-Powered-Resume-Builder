# API Endpoints Reference Guide

## Quick Navigation

- [Auth-Service (8081)](#auth-service-port-8081)
- [Resume-Service (8082)](#resume-service-port-8082)
- [Resume-Section-Service (8083)](#resume-section-service-port-8083)
- [Admin-Server (9090)](#admin-server-port-9090)

---

## Auth-Service (Port 8081)

### Swagger UI
```
http://localhost:8081/swagger-ui/index.html
http://localhost:8081/v3/api-docs
```

### Endpoints

#### User Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | User registration |
| POST | `/auth/login` | User login with credentials |
| POST | `/auth/refresh` | Refresh JWT token |
| POST | `/auth/logout` | User logout |

#### Google OAuth2
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/google/login` | Google OAuth login |
| POST | `/auth/google/callback` | Google OAuth callback |

#### Health Check
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Service health status |

---

## Resume-Service (Port 8082)

### Swagger UI
```
http://localhost:8082/swagger-ui/index.html
http://localhost:8082/v3/api-docs
```

### Endpoints

#### Health & Welcome
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/resumes/welcome` | Service health check |
| GET | `/actuator/health` | Service health status |

#### Resume Management - CRUD
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/resumes` | Create a new resume | ✓ |
| GET | `/api/v1/resumes/{id}` | Get resume by ID | Optional |
| PUT | `/api/v1/resumes/{id}` | Update resume | ✓ |
| DELETE | `/api/v1/resumes/{id}` | Delete resume | ✓ |

#### Resume by User
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/resumes/user/{userId}` | Get all resumes for user | ✓ |
| GET | `/api/v1/resumes/user/{userId}/count` | Count user's resumes | ✓ |
| GET | `/api/v1/resumes/user/{userId}/filter?isPublic={boolean}` | Filter resumes by public status | ✓ |

#### Resume Operations
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/resumes/{id}/duplicate` | Duplicate resume | ✓ |
| PUT | `/api/v1/resumes/{id}/publish?isPublic={boolean}` | Publish/unpublish resume | ✓ |

#### Public Resumes
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/resumes/public/all` | Get all public resumes | Optional |

### Request/Response Examples

**Create Resume:**
```json
POST /api/v1/resumes
Content-Type: application/json

{
  "userId": 1,
  "title": "Senior Developer Resume",
  "content": "My professional resume...",
  "isPublic": false,
  "status": "DRAFT",
  "description": "My primary resume"
}

Response (201):
{
  "id": 101,
  "userId": 1,
  "title": "Senior Developer Resume",
  "content": "My professional resume...",
  "isPublic": false,
  "viewCount": 0,
  "status": "DRAFT",
  "description": "My primary resume",
  "createdAt": "2026-04-18T12:00:00Z",
  "updatedAt": "2026-04-18T12:00:00Z"
}
```

**Get Resume by ID:**
```
GET /api/v1/resumes/101

Response (200):
{
  "id": 101,
  "userId": 1,
  "title": "Senior Developer Resume",
  ...
}
```

---

## Resume-Section-Service (Port 8083)

### Swagger UI
```
http://localhost:8083/swagger-ui/index.html
http://localhost:8083/v3/api-docs
```

### Endpoints

#### Health Check
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Service health status |

#### Section Management - CRUD
| Method | Endpoint | Description | Path Variables |
|--------|----------|-------------|-----------------|
| POST | `/api/v1/resumes/{resumeId}/sections` | Create section | resumeId |
| GET | `/api/v1/resumes/{resumeId}/sections` | Get all sections | resumeId |
| GET | `/api/v1/resumes/{resumeId}/sections/{sectionId}` | Get specific section | resumeId, sectionId |
| PUT | `/api/v1/resumes/{resumeId}/sections/{sectionId}` | Update section | resumeId, sectionId |
| DELETE | `/api/v1/resumes/{resumeId}/sections/{sectionId}` | Delete section | resumeId, sectionId |

#### Section Operations
| Method | Endpoint | Description | Path Variables |
|--------|----------|-------------|-----------------|
| GET | `/api/v1/resumes/{resumeId}/sections/type/{type}` | Get sections by type | resumeId, type |
| PATCH | `/api/v1/resumes/{resumeId}/sections/{sectionId}/visibility` | Toggle visibility | resumeId, sectionId |
| PATCH | `/api/v1/resumes/{resumeId}/sections/reorder` | Reorder sections | resumeId |
| PUT | `/api/v1/resumes/{resumeId}/sections/bulk` | Bulk update sections | resumeId |

#### Batch Operations
| Method | Endpoint | Description | Path Variables |
|--------|----------|-------------|-----------------|
| DELETE | `/api/v1/resumes/{resumeId}/sections` | Delete all sections | resumeId |

### Section Types

Valid `sectionType` values:
- `SUMMARY` - Professional summary
- `EXPERIENCE` - Work experience
- `EDUCATION` - Education details
- `SKILLS` - Technical/professional skills
- `CERTIFICATIONS` - Certifications and licenses
- `PROJECTS` - Project portfolio
- `LANGUAGES` - Languages spoken
- `VOLUNTEER` - Volunteer experience
- `CUSTOM` - Custom sections

### Request/Response Examples

**Create Section:**
```json
POST /api/v1/resumes/101/sections
Content-Type: application/json

{
  "resumeId": 101,
  "sectionType": "SUMMARY",
  "title": "Professional Summary",
  "content": "Experienced software developer with 5+ years...",
  "displayOrder": 1,
  "isVisible": true,
  "aiGenerated": false
}

Response (201):
{
  "sectionId": 201,
  "resumeId": 101,
  "sectionType": "SUMMARY",
  "title": "Professional Summary",
  "content": "Experienced software developer with 5+ years...",
  "displayOrder": 1,
  "isVisible": true,
  "aiGenerated": false,
  "createdAt": "2026-04-18T12:05:00Z",
  "updatedAt": "2026-04-18T12:05:00Z"
}
```

**Get All Sections for Resume:**
```
GET /api/v1/resumes/101/sections

Response (200):
[
  {
    "sectionId": 201,
    "resumeId": 101,
    "sectionType": "SUMMARY",
    "title": "Professional Summary",
    "displayOrder": 1,
    ...
  },
  {
    "sectionId": 202,
    "resumeId": 101,
    "sectionType": "EXPERIENCE",
    "title": "Work Experience",
    "displayOrder": 2,
    ...
  }
]
```

**Get Sections by Type:**
```
GET /api/v1/resumes/101/sections/type/EXPERIENCE

Response (200):
[
  {
    "sectionId": 202,
    "resumeId": 101,
    "sectionType": "EXPERIENCE",
    "title": "Work Experience",
    ...
  }
]
```

**Update Section:**
```json
PUT /api/v1/resumes/101/sections/201
Content-Type: application/json

{
  "resumeId": 101,
  "sectionType": "SUMMARY",
  "title": "Updated Professional Summary",
  "content": "Updated content...",
  "displayOrder": 1,
  "isVisible": true,
  "aiGenerated": false
}

Response (200):
{
  "sectionId": 201,
  "resumeId": 101,
  ...
}
```

**Reorder Sections:**
```json
PATCH /api/v1/resumes/101/sections/reorder
Content-Type: application/json

{
  "sectionIds": [201, 203, 202, 204]
}

Response (204): No Content
```

**Bulk Update Sections:**
```json
PUT /api/v1/resumes/101/sections/bulk
Content-Type: application/json

[
  {
    "resumeId": 101,
    "sectionType": "SUMMARY",
    "title": "Updated Summary",
    "displayOrder": 1
  },
  {
    "resumeId": 101,
    "sectionType": "EXPERIENCE",
    "title": "Updated Experience",
    "displayOrder": 2
  }
]

Response (200):
[
  { "sectionId": 201, ... },
  { "sectionId": 202, ... }
]
```

**Toggle Visibility:**
```
PATCH /api/v1/resumes/101/sections/201/visibility

Response (200):
{
  "sectionId": 201,
  "isVisible": false,
  ...
}
```

**Delete Section:**
```
DELETE /api/v1/resumes/101/sections/201

Response (204): No Content
```

**Delete All Sections for Resume:**
```
DELETE /api/v1/resumes/101/sections

Response (204): No Content
```

---

## Admin-Server (Port 9090)

### Dashboard
```
http://localhost:9090/
```

### Features
- Real-time application monitoring
- Service health status
- Logs and metrics visualization
- Endpoint statistics
- Thread dump analysis

### Registered Applications
- ADMIN-SERVICE
- API-GATEWAY
- AUTH-SERVICE
- RESUME-SERVICE
- RESUME-SECTION-SERVICE

---

## Common Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2026-04-18T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid input data",
  "path": "/api/v1/resumes"
}
```

### 404 Not Found
```json
{
  "timestamp": "2026-04-18T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Resume not found",
  "path": "/api/v1/resumes/999"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2026-04-18T12:00:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/v1/resumes"
}
```

---

## Testing with cURL

### Create Resume
```bash
curl -X POST http://localhost:8082/api/v1/resumes \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "title": "Test Resume",
    "status": "DRAFT"
  }'
```

### Get Resume
```bash
curl -X GET http://localhost:8082/api/v1/resumes/1
```

### Create Section
```bash
curl -X POST http://localhost:8083/api/v1/resumes/1/sections \
  -H "Content-Type: application/json" \
  -d '{
    "resumeId": 1,
    "sectionType": "SUMMARY",
    "title": "Summary",
    "displayOrder": 1,
    "isVisible": true
  }'
```

### Get Sections
```bash
curl -X GET http://localhost:8083/api/v1/resumes/1/sections
```

### Update Section
```bash
curl -X PUT http://localhost:8083/api/v1/resumes/1/sections/1 \
  -H "Content-Type: application/json" \
  -d '{
    "resumeId": 1,
    "sectionType": "SUMMARY",
    "title": "Updated Summary",
    "displayOrder": 1
  }'
```

### Delete Section
```bash
curl -X DELETE http://localhost:8083/api/v1/resumes/1/sections/1
```

---

## Database Queries

### Get All Resumes for User
```sql
SELECT * FROM resumes WHERE user_id = 1;
```

### Get All Sections for Resume
```sql
SELECT * FROM resume_sections WHERE resume_id = 101 ORDER BY display_order;
```

### Get Sections by Type
```sql
SELECT * FROM resume_sections 
WHERE resume_id = 101 AND section_type = 'EXPERIENCE'
ORDER BY display_order;
```

### Count Resumes by User
```sql
SELECT COUNT(*) FROM resumes WHERE user_id = 1;
```

---

## Performance Tips

1. **Pagination for large result sets**
   - Implement offset/limit parameters for section lists
   - Cache frequently accessed resumes

2. **Database Indexing**
   - Index on `user_id` in resumes table
   - Index on `resume_id` in resume_sections table
   - Composite index on (resume_id, display_order)

3. **API Optimization**
   - Use bulk operations for updating multiple sections
   - Minimize unnecessary cross-service calls
   - Implement response caching with Redis

---

## Security Best Practices

1. **Authentication**
   - All user-specific endpoints require JWT token
   - Pass token in `Authorization: Bearer {token}` header

2. **Authorization**
   - Users can only access their own resumes
   - Sections must belong to the specified resume
   - Validate ownership before deletion

3. **Data Validation**
   - All inputs validated using Jakarta validation
   - Content size limits enforced (50000 chars max)
   - Section type enum validation

4. **SQL Injection Prevention**
   - Use parameterized queries (Hibernate/JPA)
   - No raw SQL queries on user input

---

## Troubleshooting

### Issue: Cannot access Swagger UI on port 8083
**Solution:** Restart resume-section-service. SecurityConfig was missing and has been added.

### Issue: Section not created without resumeId
**Solution:** resumeId is required and must match the path parameter: `/api/v1/resumes/{resumeId}/sections`

### Issue: Cannot delete resume because sections exist
**Solution:** Delete sections first via `/api/v1/resumes/{resumeId}/sections` or use cascading delete endpoint

### Issue: 401 Unauthorized error
**Solution:** Include valid JWT token in Authorization header: `Authorization: Bearer {token}`


