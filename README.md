# Patient Management Service

Patient/auth microservice for registration, profiles, and document/report related APIs.

## Prerequisites
- Java 17
- Maven
- Docker Desktop

## Local Setup
1. Start MongoDB:

```bash
docker-compose up -d
```

2. Build and run:

```bash
mvnw.cmd clean package -DskipTests
mvnw.cmd spring-boot:run
```

## Default Runtime
- Service URL: http://localhost:8081

## Common Endpoints
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/patient/profile`

## Environment Variables
- `SPRING_DATA_MONGODB_URI`
- `JWT_SECRET`
- `JWT_EXPIRATION`

## Docker Run
```bash
mvnw.cmd clean package -DskipTests
docker-compose up --build
```

## Stop
```bash
docker-compose down
```

