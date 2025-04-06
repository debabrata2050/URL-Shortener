# 🔗 d50.in | Cloud-Deployed URL Shortener

🚀 A full-stack URL shortening service built with Spring Boot (Java) and deployed on AWS EC2 using Nginx as a reverse proxy and MySQL (RDS) for persistence. Convert long URLs into short, manageable aliases under the custom `d50.in` domain.

[![Live Demo](https://img.shields.io/badge/Live_Demo-d50.in-informational?style=flat-square&logo=googlechrome)](https://d50.in)

---

## ✨ Key Features

*   **🔗 URL Shortening:** Converts long, cumbersome URLs into neat short links.
*   **✍️ Custom Aliases:** Allows users to suggest a preferred short alias (e.g., `d50.in/r/my-project`).
*   **🎲 Random Aliases (Base62):** Generates unique, 7-character Base62 (`[0-9a-zA-Z]`) aliases automatically if no custom alias is provided.
*   **♻️ Duplicate URL Handling:** If a long URL has already been shortened, the existing short link is returned, preventing duplicate entries.
*   **➡️ Secure Redirection:** Uses HTTP 301 Permanent Redirects for optimal SEO and performance when redirecting from the short link (`/r/{alias}`) to the original URL.
*   **🖥️ Simple Frontend:** A clean web interface built with HTML, CSS, and Vanilla JavaScript for easy submission of URLs.
*   **🔒 HTTPS Enforced:** All traffic is automatically redirected to HTTPS, secured using Nginx and free SSL certificates from Let's Encrypt.
*   **☁️ Cloud Native:** Designed and deployed on AWS infrastructure (EC2, RDS, Route 53).

---

## 💻 Technology Stack

Here's a breakdown of the technologies used across the project:

### ☕ Backend (Server-Side Logic)
[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Hibernate](https://img.shields.io/badge/Hibernate-ORM-59666C?style=flat-square&logo=hibernate&logoColor=white)](https://hibernate.org/orm/)

### ✨ Frontend (User Interface)
[![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=flat-square&logo=html5&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/HTML5)
[![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=flat-square&logo=css3&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/CSS)
[![JavaScript](https://img.shields.io/badge/JavaScript-ES6+-F7DF1E?style=flat-square&logo=javascript&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/JavaScript)

### 💾 Database & Persistence
[![H2](https://img.shields.io/badge/H2_Database-09476B?style=flat-square&logo=h2database&logoColor=white)](https://www.mysql.com/)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![AWS RDS](https://img.shields.io/badge/AWS%20RDS%20(MySQL)-Prod%20DB-FF9900?style=flat-square&logo=amazonrds&logoColor=white)](https://aws.amazon.com/rds/)
<br />*(Note: H2 Database is used for local development/testing.)*

### ☁️ Deployment, Networking, Proxy & Security
[![AWS EC2](https://img.shields.io/badge/AWS%20EC2-Compute-FF9900?style=flat-square&logo=amazonec2&logoColor=white)](https://aws.amazon.com/ec2/)
[![AWS Route53](https://img.shields.io/badge/AWS%20Route%2053-DNS-FF9900?style=flat-square&logo=amazonroute53&logoColor=white)](https://aws.amazon.com/route53/)
[![Nginx](https://img.shields.io/badge/Nginx-Proxy-009639?style=flat-square&logo=nginx&logoColor=white)](https://nginx.org/en/)
[![Let's Encrypt](https://img.shields.io/badge/Let's%20Encrypt-SSL-003A70?style=flat-square&logo=letsencrypt&logoColor=white)](https://letsencrypt.org/)

### 🛠️ Build Tools
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Gradle](https://img.shields.io/badge/Gradle-Build-02303A?style=flat-square&logo=gradle&logoColor=white)](https://gradle.org/)
<br />*(Choose one or both depending on your project setup)*

---

## 🏗️ Architecture Overview

The application follows a standard client-server architecture enhanced by a reverse proxy:

1.  **User's Browser:** Interacts with the static frontend (HTML/CSS/JS).
2.  **Nginx (on EC2):**
    *   Listens on ports 80 (HTTP) & 443 (HTTPS).
    *   Redirects all HTTP traffic to HTTPS.
    *   Terminates SSL for HTTPS traffic using Let's Encrypt certificates.
    *   Forwards valid HTTPS requests for `/api/*` or `/r/*` to the Spring Boot application running internally on port 8080.
    *   Adds crucial `X-Forwarded-*` headers.
3.  **Spring Boot Application (on EC2):**
    *   Listens internally on `localhost:8080`.
    *   Handles API requests (`/api/shorten`) and redirection logic (`/r/{alias}`).
    *   Interacts with the database via Spring Data JPA.
    *   Configured to understand `X-Forwarded-*` headers.
4.  **AWS RDS (MySQL Database):**
    *   Stores the `alias` -> `long URL` mappings persistently.
    *   Accessible only from the EC2 instance via internal VPC networking (secured by Security Groups).

*(Diagram Recommendation: Insert a clear architecture diagram image here showing the flow: User -> Browser -> Route 53 -> EC2 (Nginx -> Spring Boot) -> RDS)*

---

## 🔌 API Endpoints

| Method | Endpoint        | Request Body / Path Params           | Success Response                             | Error Response(s)                                          | Description                                      |
| :----- | :-------------- | :----------------------------------- | :------------------------------------------- | :--------------------------------------------------------- | :----------------------------------------------- |
| `POST` | `/api/shorten`  | JSON (`CreateRedirectRequest` DTO)   | 201 Created - JSON (`RedirectResponse` DTO)  | 400 Bad Request (Validation, Alias Exists), 503 (Alias Gen) | Creates a new short URL or returns existing one. |
| `GET`  | `/r/{alias}`    | Path Variable: `alias` (String)      | 301 Moved Permanently (`Location` Header)  | 404 Not Found                                              | Redirects the user to the original long URL.     |

**DTOs:**
*   `CreateRedirectRequest`: `{ "url": "string (required)", "alias": "string (optional, max 10 chars, [a-zA-Z0-9-])" }`
*   `RedirectResponse`: `{ "alias": "string", "originalUrl": "string", "shortUrl": "string (full https://d50.in/r/alias)" }`

---

## 🗄️ Database Schema

**Table: `redirects`**

| Column Name     | Data Type        | Constraints                              | Description                      |
| :-------------- | :--------------- | :--------------------------------------- | :------------------------------- |
| `id`            | `BIGINT`         | **Primary Key**, **Auto Increment**      | Unique record identifier.        |
| `alias`         | `VARCHAR(10)`    | **Unique**, **Not Null**                 | The short alias string.          |
| `url`           | `VARCHAR(2048)`  | **Not Null**                             | The original long URL.           |
| `created_at`    | `TIMESTAMP`      | **Not Null**, Defaults to Current Time | Timestamp of record creation.    |

---

## 📝 Blog Post

**Read more about the development process, challenges faced, and deployment details in my blog post:**

➡️ **[Link to Your Blog Post Here]** (Coming Soon!)
---
