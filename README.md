
# AI-Enabled Smart Healthcare Platform (Microservices)

Cloud-native telemedicine platform like Channeling.lk using Spring Boot microservices, MongoDB, Docker, and Kubernetes. Supports patient/doctor/admin roles, appointments, video calls, payments, and AI symptom checker.

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-7.0-green.svg)](https://mongodb.com)
[![Docker](https://img.shields.io/badge/Docker-24-blue.svg)](https://docker.com)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-1.28-purple.svg)](https://kubernetes.io)

## Architecture Overview
High-level diagram: [Insert PlantUML/image here or link to report.pdf]

Microservices:
- **Patient Management** (port 8081): Registration, profiles, reports, prescriptions, user mgmt.
- **Doctor Management** (8082): Profiles, availability.
- **Appointment** (8083): Booking, status.
- **Telemedicine** (8084): Video via Jitsi/Twilio.
- **Payment** (8085): PayHere/Stripe.
- **Notification** (8086): SMS/Email.
- **AI Symptom Checker** (optional, 8087).

Communication: REST APIs + API Gateway (Spring Cloud Gateway).

## Tech Stack
- Backend: Spring Boot 3.x, MongoDB
- Frontend: React/Angular (async client)
- Auth: JWT
- Container: Docker
- Orchestration: Kubernetes
- Others: Multer/GridFS for files, WebRTC for video.

## Quick Start (Local)

1. **Prerequisites**:
   ```
   Docker, Docker Compose, Minikube/Kubectl, Node.js, Maven
   MongoDB: `docker compose up -d` (see `docker-compose.yml`)
   ```

2. **Start MongoDB (Local)**:
   ```
   docker compose up -d
   ```

3. **Clone & Build**:
   ```
   git clone <your-org>/<repo>
   cd patient-management-service
   mvn clean package -DskipTests
   ```

3. **Docker Compose (Dev)**:
   ```
   docker-compose up -d  # Starts MongoDB + services
   ```

4. **Kubernetes (Minikube)**:
   ```
   minikube start
   eval $(minikube docker-env)
   kubectl apply -f k8s/
   minikube dashboard  # View services
   ```

5. **Access**:
   - API: http://localhost:8081/api/auth/register
   - Frontend: http://localhost:3000 (serve React build)

## Deployment Steps (Production - AWS Free Tier/EC2)
1. Push Docker images to ECR: `aws ecr create-repo patient-service`
2. Update `k8s/deployment.yaml` with ECR image.
3. Launch EC2 t2.micro (Free Tier), install Docker/K8s (k3s).
4. `kubectl apply -f k8s/`
5. Expose via LoadBalancer or Ingress.

**AWS Free Tier Note**: Use EC2/ECS for dev; EKS costs ~$73/month.

## API Documentation
See `/openapi.html` or Postman collection in `docs/`.

**Patient Management Endpoints** (example):
- POST `/api/auth/register` - {role: "PATIENT", ...}
- GET `/api/patient/profile` - Bearer JWT

## Testing
```
mvn test  # Unit/Integration
docker exec -it <container> mvn test
```

## Group Members
See members.txt

## Contributions
- Member1 (Reg#): Patient Service
- etc.

**Demo Video**: https://youtube.com/...

For issues: Create GitHub issue.

