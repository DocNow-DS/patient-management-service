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
