# 🔗 d50.in | High-Performance URL Shortener & Click Analytics Platform

🚀 A production-grade URL shortener and async analytics platform built with **Java 25**, **Spring Boot 4.1.1**, **MySQL 9.x**, **Redis**, and **RabbitMQ**.

---

## ✨ Key Features & Architecture

*   **➡️ HTTP 302 Found Redirection:** Replaced permanent 301 redirects with temporary HTTP 302 redirects (`no-cache` headers) so client browsers never cache redirects locally, ensuring 100% of click events hit your server for live analytics.
*   **⚡ Two-Tier (L1/L2) Hierarchical Caching:**
    *   **L1 (Caffeine Local RAM):** Sub-millisecond lookup ($\approx 10\text{--}50\,\mu\text{s}$) for ultra-hot links.
    *   **L2 (Redis Distributed Cache):** $1\text{--}3\,\text{ms}$ network cache with dynamic TTL matching link expiration.
    *   **L3 (MySQL Persistent Store):** Indexed lookup on `short_code`.
    *   **Cache Penetration Protection:** Negative sentinel caching (60s TTL) for probing attacks on nonexistent URLs.
    *   **Thundering Herd Mitigation:** Striped locking per key serializes concurrent database lookups.
    *   **Redis Pub/Sub Invalidation:** Cluster-wide cache eviction across instances when links are updated.
*   **📨 Asynchronous Analytics Pipeline (RabbitMQ):**
    *   **Non-Blocking Redirects:** The redirect path extracts `User-Agent`, `Referer`, and client IP headers and pushes a message to RabbitMQ in $< 5\text{ms}$.
    *   **Broker Outage Fallback:** In the event of a RabbitMQ outage, events are buffered in an in-memory bounded circular queue without failing the redirect.
    *   **Dead-Letter Queue (DLQ):** Messages that fail processing are routed to `click.events.dlq`.
    *   **Background Consumer:** `@RabbitListener` worker parses user agents, filters web crawlers/bots (`Googlebot`, `Bingbot`, `Slackbot`, etc.), resolves geographic location, and writes micro-batches (50 records or 500ms) to MySQL.
*   **🎲 Pluggable Base62 Key Generation:**
    *   **Counter-Based Range Allocator:** Thread-safe `AtomicLong` memory range allocator converted to 6-char Base62 using zero-dependency arithmetic.
    *   **Randomized Key Generator:** Cryptographic `SecureRandom` Base62 generator with collision retries.
    *   **Custom Vanity Slugs:** Supports custom aliases with reserved route protection (`/api`, `/admin`, `/r`, `/metrics`, `/actuator`).
*   **🛡️ SSRF Defense & Security:**
    *   Rejects local loopbacks (`127.0.0.1`, `::1`, `localhost`).
    *   Rejects private RFC 1918 subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`).
    *   Rejects cloud instance metadata service (`169.254.169.254`).
*   **📊 Live Analytical Reporting REST APIs:**
    *   Total clicks, unique IP visitors, bot traffic ratio.
    *   Top 5 referrers with SQL aggregations and Java Stream percentage rankings.
    *   Top 5 countries and cities.
    *   Device breakdown (Mobile, Desktop, Tablet, Bot).
*   **🧪 83 Passing Automated Tests:**
    *   Full JUnit 5 & Mockito test suite covering boundaries, SSRF, service layers, cache behavior, broker fallback resilience, and MockMvc endpoints.

---

## 💻 Technology Stack

*   **Runtime:** Java 25 (LTS)
*   **Framework:** Spring Boot 4.1.1 (Spring Framework 7.x, Spring Data JPA, Spring AMQP, Spring Data Redis, Spring Cache)
*   **Database:** MySQL 9.x (`com.mysql:mysql-connector-j`) with versioned Flyway migrations (`flyway-mysql`)
*   **Caches:** Caffeine 3.x (L1) + Redis 7.x (L2)
*   **Message Broker:** RabbitMQ 3.13+ (Topic Exchange + DLQ)
*   **Testing:** JUnit 5, Mockito, AssertJ, MockMvc, H2 (test scope)
*   **Build Tool:** Apache Maven 3.9.16 via `.\mvnw.cmd`

---

## 🔌 API Endpoints

### 1. Redirection Endpoint
| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `GET` | `/r/{shortCode}` | Resolves link and redirects client | **302 Found** (`Location` header) |

### 2. Link Management Endpoints
| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/links` | `{"originalUrl":"...", "customSlug":"...", "expiresInDays":7}` | Creates short link |
| `GET` | `/api/v1/links` | None | Lists all links |
| `GET` | `/api/v1/links/{shortCode}` | None | Gets short link details |
| `PATCH`| `/api/v1/links/{shortCode}/deactivate` | None | Deactivates short link and clears cache |
| `PUT`  | `/api/v1/links/{shortCode}/destination` | `{"originalUrl":"..."}` | Updates target URL and evicts caches |
| `POST` | `/api/shorten` | `{"url":"...", "alias":"..."}` | Legacy backward-compatible endpoint |

### 3. Analytics Endpoints
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/analytics/links/{shortCode}/summary` | Total clicks, bot count, unique visitors, bot % |
| `GET` | `/api/v1/analytics/links/{shortCode}/referrers?limit=5` | Top referrers with click counts and percentages |
| `GET` | `/api/v1/analytics/links/{shortCode}/countries?limit=5` | Top countries with click counts and percentages |
| `GET` | `/api/v1/analytics/links/{shortCode}/devices` | Breakdown by device type (Mobile/Desktop/Tablet) |

---

## 🚀 Running Locally

### 1. Run Unit Tests (In-Memory H2, No external services required)
```powershell
.\mvnw.cmd test
```

### 2. Start Supporting Services (Redis & RabbitMQ via Docker)
```powershell
docker compose up -d redis rabbitmq
```

### 3. Launch the Application
Make sure local MySQL service `MySQL93` is running on port 3306:
```powershell
.\mvnw.cmd spring-boot:run
```
Application starts on `http://localhost:8080`.
RabbitMQ Management UI is at `http://localhost:15672` (guest / guest).
