# Enterprise RBAC System

A full-stack Role-Based Access Control (RBAC) system for enterprise applications, featuring a Spring Boot backend and a React/Ant Design frontend.

## Features
- JWT authentication (login, logout, refresh)
- Password reset and email verification
- User, role, screen, session, and permission management
- Granular permission management (API, data, component, system, etc.)
- Component-level UI enforcement
- Audit logging and security event monitoring
- Reports, analytics, dashboards, and system health monitoring
- Global notification system
- Modern, responsive UI

## Backend (Spring Boot)
- Java 17+, Spring Boot, JPA/Hibernate, Oracle DB
- RESTful APIs under `/api/v1/`
- Setup:
  1. Configure `application.properties` for your Oracle DB.
  2. Build and run: `./mvnw spring-boot:run`
- Key Endpoints:
  - `/api/v1/auth/*` (auth, profile, password/email reset)
  - `/api/v1/admin/*` (users, roles, permissions, screens, sessions, audit logs)
  - `/api/v1/reports/*` (reports, analytics)
  - `/api/v1/system/*` (health, stats)

## Frontend (React)
- React 19+, Ant Design 5+
- Setup:
  1. `cd frontend`
  2. `npm install`
  3. `npm start`
- Features:
  - Modern UI with sidebar navigation
  - Protected routes and dynamic navigation
  - CRUD for all entities
  - Dashboard, reports, system health, and profile pages
  - Global notifications

## Security
- Passwords are BCrypt-hashed
- JWTs are used for all API calls
- Permissions enforced on both backend and frontend

## Customization
- Add new permission types, screens, or components as needed
- Extend audit logging and analytics as your needs grow

## License
MIT or your company license 