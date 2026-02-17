# Fiverr Sharable Links Service

A backend service that powers short, trackable URLs for Fiverr sellers. Sellers generate short links for their Gigs, share them on social media, and earn Fiverr credits for valid clicks.

## Setup

### Prerequisites

- **Java 21** (OpenJDK Temurin recommended)
- **Docker Desktop** (for PostgreSQL)
- **Maven 3.9+** (or use the included `./mvnw` wrapper)

### Environment Variables

No external env vars are needed. Database configuration is in `src/main/resources/application.properties`:

| Property | Value |
|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/interviewdb` |
| `spring.datasource.username` | `admin` |
| `spring.datasource.password` | `admin` |

### Run Locally

1. **Start PostgreSQL** (Docker):

```bash
docker run --name interview-postgres \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin \
  -e POSTGRES_DB=interviewdb \
  -p 5432:5432 \
  -d postgres:16
```

2. **Build and run the app:**

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.

## Architecture

```
src/main/java/com/interview/interview_project/   (package: com.interview.interview_project)
  |-- FiverrShortlinksApplication.java    # Entry point, @EnableAsync
  |-- model/
  |     |-- Link.java                     # Short link entity (links table)
  |     |-- ClickEvent.java               # Click tracking entity (click_events table)
  |-- repository/
  |     |-- LinkRepository.java           # Link CRUD + lookup by shortCode/targetUrl
  |     |-- ClickEventRepository.java     # Click CRUD + aggregation queries
  |-- service/
  |     |-- LinkService.java              # Link creation (idempotent), stats aggregation
  |     |-- ClickTrackingService.java     # Async click processing + credit award
  |     |-- FraudValidationService.java   # Simulated fraud check (500ms, 50% probability)
  |-- controller/
  |     |-- LinkController.java           # REST endpoints (POST /links, GET /:shortCode, GET /stats)
  |-- dto/
  |     |-- CreateLinkRequest.java        # Input DTO for POST /links
  |     |-- CreateLinkResponse.java       # Output DTO for POST /links
  |     |-- LinkStatsResponse.java        # Output DTO for GET /stats
  |     |-- MonthlyBreakdown.java         # Monthly earnings sub-object
  |-- exception/
        |-- GlobalExceptionHandler.java   # Centralized error handling (400/404/500)
        |-- LinkNotFoundException.java    # Thrown when short code doesn't exist
```

### How Components Interact

1. **POST /links** -> `LinkController` -> `LinkService.createShortLink()` -> checks if URL exists (`LinkRepository.findByTargetUrl`), creates if not -> returns short URL.

2. **GET /:shortCode** -> `LinkController` -> `LinkService.getByShortCode()` -> returns 302 redirect immediately -> `ClickTrackingService.processClick()` runs asynchronously: records click, runs fraud validation (500ms), awards $0.05 credit if valid.

3. **GET /stats** -> `LinkController` -> `LinkService.getStats()` -> fetches paginated links, aggregates click counts, total earnings, and monthly breakdowns from `ClickEventRepository`.

### Data Model

- **links**: `id`, `short_code` (unique), `target_url` (unique), `created_at`
- **click_events**: `id`, `link_id` (FK -> links), `clicked_at`, `fraud_valid`, `credit_awarded`

## API Reference

### POST /links
Create a short link. Idempotent (same URL returns same short link).

```bash
curl -X POST http://localhost:8080/links \
  -H "Content-Type: application/json" \
  -d '{"targetUrl": "https://fiverr.com/seller/my-gig"}'
```

Response (201):
```json
{
  "shortUrl": "http://localhost:8080/abc1234",
  "targetUrl": "https://fiverr.com/seller/my-gig"
}
```

### GET /:shortCode
Redirects to the target URL (302). Tracks the click asynchronously.

```bash
curl -v http://localhost:8080/abc1234
# Returns: 302 Found, Location: https://fiverr.com/seller/my-gig
```

### GET /stats
Paginated analytics for all links.

```bash
curl "http://localhost:8080/stats?page=0&size=10"
```

Response (200):
```json
{
  "content": [
    {
      "url": "https://fiverr.com/seller/my-gig",
      "totalClicks": 16,
      "totalEarnings": 0.40,
      "monthlyBreakdown": [
        {"month": "01/2026", "earnings": 0.35},
        {"month": "02/2026", "earnings": 0.05}
      ]
    }
  ],
  "totalPages": 1,
  "totalElements": 1
}
```

## Testing

### Automated Tests

```bash
./mvnw test
```

Runs 22 tests:
- **Unit tests**: `LinkServiceTest` (8 tests) -- short code generation, idempotent creation, validation, error handling
- **Unit tests**: `FraudValidationServiceTest` (3 tests) -- timing, randomness
- **Integration tests**: `LinkControllerIntegrationTest` (11 tests) -- full HTTP endpoint testing with H2 in-memory database

### Manual Testing (Postman)

1. **Create a link**: POST `http://localhost:8080/links` with body `{"targetUrl": "https://fiverr.com/test"}`
2. **Click it**: GET the returned `shortUrl` in a browser or Postman (follow redirects off to see the 302)
3. **Check stats**: GET `http://localhost:8080/stats`

## AI Environment Setup

### IDE: Cursor

- AI-assisted code generation via Cursor's built-in chat
- Used for scaffolding entities, services, tests, and documentation

### Plugins and Configuration

- **Spring Boot support**: Built-in Java/Spring Boot language server for autocomplete and error detection
- **Database tools**: DbVisualizer connected to `localhost:5432/interviewdb` for inspecting tables and data

### Custom Improvements

- **Execution Safety Protocol**: A step-by-step plan with explicit verification at each stage
- **Backup/Restore strategy**: All system config files backed up before modifications, with a tested revert procedure
