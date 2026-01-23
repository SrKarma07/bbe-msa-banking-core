# BBE Banking Core — Reactive Microservices (Customer + Account/Movement)

A production-oriented **reactive backend** built with **Spring Boot 3**, **Java 17**, **WebFlux**, **R2DBC**, and **PostgreSQL**.  
This repository contains **two independent microservices** that share the same database schema:

- **bbe-msa-bs-customer-management**: Customer and Person management.
- **bbe-msa-bs-account-movement**: Account management, movement registration, and account statement/reporting features.

The project follows an **OpenAPI-first** approach (contracts stored in `src/main/resources/*-openapi.*`) and generates server stubs/models using **OpenAPI Generator** during the build. API documentation is available through **Swagger UI**.

---

## Table of Contents

- [1. Architecture & Tech Stack](#1-architecture--tech-stack)
- [2. Repository Structure](#2-repository-structure)
- [3. Prerequisites](#3-prerequisites)
- [4. Clone & Run with Docker (Recommended)](#4-clone--run-with-docker-recommended)
- [5. API Documentation (Swagger UI)](#5-api-documentation-swagger-ui)
- [6. Postman Collections](#6-postman-collections)
- [7. Local Development (Without Docker)](#7-local-development-without-docker)
- [8. Testing](#8-testing)
- [9. Troubleshooting](#9-troubleshooting)
- [10. Author](#10-author)

---

## 1. Architecture & Tech Stack

### Core Technologies
- **Language**: Java 17
- **Framework**: Spring Boot 3
- **Reactive Web**: Spring WebFlux (Project Reactor)
- **Database Access**: Spring Data **R2DBC**
- **Database**: PostgreSQL 16
- **Build Tool**: Gradle (Wrapper included)
- **API Contracts**: OpenAPI 3 (contract-first)
- **Code Generation**: openapi-generator (Gradle plugin)
- **Mapping**: MapStruct + Lombok
- **API Docs**: SpringDoc / Swagger UI
- **Containers**: Docker + Docker Compose
- **Integration Tests**: JUnit 5 + **Testcontainers** (PostgreSQL)

### Architectural Notes
- The services are built using **Reactive Programming** (Spring WebFlux + Project Reactor) and follow a **Hexagonal Architecture (Ports & Adapters)** approach to keep domain logic isolated from infrastructure concerns (HTTP, persistence, external integrations).

- Each microservice is **independently deployable** and owns its runtime configuration.
- Both services connect to the **same database** (shared schema) for this exercise.
- Contract-first development ensures the API is always aligned with the OpenAPI definitions.

---

## 2. Repository Structure

```text
bbe-msa-banking-core/
├─ BaseDatos.sql
├─ docker-compose.yml
├─ bbe-msa-bs-customer-management/
│  ├─ Dockerfile
│  ├─ build.gradle.kts
│  ├─ gradlew / gradlew.bat
│  └─ src/
│     └─ main/resources/
│        ├─ bbe-msa-bs-customer-management-openapi.yaml
│        └─ bbe-msa-bs-customer-management-application.yml
├─ bbe-msa-bs-account-movement/
│  ├─ Dockerfile
│  ├─ build.gradle.kts
│  ├─ gradlew / gradlew.bat
│  └─ src/
│     └─ main/resources/
│        ├─ bbe-msa-bs-account-movement-openapi.yml
│        └─ bbe-msa-bs-account-movement-application.yml
├─ bbe-msa-bs-customer-management (v1.0.0).postman_collection.json
└─ bbe-msa-bs-account-movement (v1.0.0).postman_collection.json

```
## 3. Prerequisites

### Required (Docker workflow)
- **Docker Desktop** (or **Docker Engine**)
- **Docker Compose v2+**

### Optional (Local development without Docker)
- **JDK 17** installed
- **IDE:** IntelliJ IDEA *(recommended)*
- **PostgreSQL 16** locally *(or any compatible version)*

---

## 4. Clone & Run with Docker (Recommended)

### 4.1 Clone the repository
```bash
git clone https://github.com/SrKarma07/bbe-msa-banking-core.git
cd bbe-msa-banking-core
```
> If you received a ZIP, extract it and cd into the extracted folder.


### 4.2 Start the project (BD + both microservices)
```bash
docker compose up -d --build
```

### 4.3 Verify containers
```bash
docker compose ps
docker compose logs -f postgres
docker compose logs -f bbe-msa-bs-customer-management
docker compose logs -f bbe-msa-bs-account-movement
```

### 4.4 Database initialization
On first startup, PostgreSQL runs:
- **BaseDatos.sql** → creates schema and inserts seed data.

The database container uses:
- **DB** → `proof-bank`
- **User** → `postgres`
- **Password** → `root`
- **Port** → `5432`

### 4.5 Ports (IMPORTANT)
Your Dockerfiles expose the following ports:
- **customer-management** `exposes 8091`
- **account-movement** `exposes 8090`
```bash
bbe-msa-bs-customer-management:
  ports:
    - "8091:8091"

bbe-msa-bs-account-movement:
  ports:
    - "8090:8090"
```

## 5. API Documentation (Swagger UI)
Once the stack is running, open Swagger UI in your browser.

### Customer Management
#### Swagger UI:

- http://localhost:8091/swagger-ui.html

- (If redirected) http://localhost:8091/swagger-ui/index.html

#### OpenAPI JSON:

- http://localhost:8091/v3/api-docs

### Account / Movement

#### Swagger UI:

- http://localhost:8090/swagger-ui.html

- (If redirected) http://localhost:8090/swagger-ui/index.html

#### OpenAPI JSON:

- http://localhost:8090/v3/api-docs

## 6. Postman Collections
This repository includes two Postman collections at the root:
- `bbe-msa-bs-customer-management (v1.0.0).postman_collection.json`

- `bbe-msa-bs-account-movement (v1.0.0).postman_collection.json`
## 6.1 Import into Postman

### 1) Open Postman
- Launch **Postman** on your computer.

### 2) Import the collections
1. Go to **Import**
2. Select **both `.json` collection files**
3. Confirm the import

### 3) Create an environment (recommended)
1. Go to **Environments**
2. Click **Create Environment**
3. Add the following variables:

| Variable        | Value                       |
|----------------|-----------------------------|
| customerBaseUrl | `http://localhost:8091/api` |
| accountBaseUrl  | `http://localhost:8090/api` |

## 6.2 Run requests

- **Customer endpoints** should point to:  
  `{{customerBaseUrl}}/...`

- **Account/Movement endpoints** should point to:  
  `{{accountBaseUrl}}/...`

> If your collection uses different variable names, simply map them accordingly in the environment.

## 7. Local Development (Without Docker)

### 7.1 Database

Create a PostgreSQL database and run:

```bash
psql -U postgres -d proof-bank -f BaseDatos.sql
```

### 7.2 Run each microservice

Each microservice is a standalone Gradle project.

##### Customer Management
```bash
cd bbe-msa-bs-customer-management
./gradlew clean openApiGenerate bootRun
```

##### Account / Movement
```bash
cd ../bbe-msa-bs-account-movement
./gradlew clean openApiGenerate bootRun
```

## 8. Testing

### 8.1 Unit tests

Run inside each microservice:

```bash
./gradlew test
```

### 8.2 Integration tests (Testcontainers + PostgreSQL)

Both services include an integrationTest task to run true integration tests with a real PostgreSQL container.
```bash
./gradlew integrationTest
```
#### What integration tests validate:

- Application boots with real configuration

- HTTP layer (controllers/handlers) works end-to-end

- Persistence layer (R2DBC repositories) works against PostgreSQL

- Data constraints and persistence behavior are correct

### 8.3 Reports

After running tests, Gradle reports can be found in:

- `build/reports/tests/test/index.html*`

- `build/reports/tests/integrationTest/index.html*`

### 9. Author

Created by **Jeremy Torres Páez**.