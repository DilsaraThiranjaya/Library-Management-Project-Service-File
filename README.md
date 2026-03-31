# Services: File Service

### Student Information
- **Student Name**: Dilsara Thiranjaya
- **Student Number**: 2301692050
- **Slack Handle**: Dilsara Thiranjaya
- **GCP Project ID**: dilsara

---


This repository contains the Spring Boot File Service microservice for the Library Management System.

## Architectural Purpose

This service is responsible for managing file uploads directly to Google Cloud Storage (GCS). It utilizes a simplified architecture suitable for an integration service:
-   **Controllers:** Expose standard REST endpoints (`/api/files/upload` and `/api/files/list`).
-   **Integration:** Injects the Spring Cloud GCP `Storage` bean to stream files securely to a configured GCS bucket.

## Technical Stack

-   **Java**: 17 / 25
-   **Spring Boot**: 4.0.3 (or as per parent POM)
-   **Spring Cloud**: 2025.1.0 (Eureka / Config)
-   **GCP SDK**: `spring-cloud-gcp-starter-storage`

## Configuration

It pulls configurations actively from the Config Server running on `config.platform:9000` (resolvable to `localhost:8888` locally). It automatically attempts to bind to Eureka at `http://localhost:8761/eureka/`. 
The target GCS bucket is configurable via the `app.gcs-bucket` property.

## Running Locally

To run this service locally, you must provide valid Google Cloud Application Default Credentials (ADC) since the `Storage` autoconfiguration expects a valid secure context.

### 1. Authenticate locally with GCP

Run the following Google Cloud CLI command and log in:
```bash
gcloud auth application-default login
```

### 2. Compile & Run Application

```bash
mvn clean compile
mvn spring-boot:run
```