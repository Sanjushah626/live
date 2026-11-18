# MyShareApp — Spring Boot 3 + YONEPSE Edition

Full-stack Spring Boot 3 application (Thymeleaf + Spring Security + Spring Data JPA + MySQL/H2) powered by the **YONEPSE API** (`https://shubhamnpk.github.io/yonepse/pages/docs.html`) with fixed Admin Console layout and upgraded responsive UIs.

## Quick Start (Built-in H2 In-Memory Mode)
Requires **Java 17+** and **Maven 3.9+**:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

Open **`http://localhost:8080`** in your browser.

## Run with MySQL
1. Start MySQL on `localhost:3306` (optional: run `schema.sql`).
2. Run:
   ```bash
   mvn spring-boot:run
   ```
   Configure `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, or `YONEPSE_BASE_URL` via environment variables or `src/main/resources/application.properties`.
