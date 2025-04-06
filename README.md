# 🔗 d50.in - Dynamic Cloud-Deployed URL Shortener

<!-- Optional Badges - Replace with your actual badges if configured -->
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](...)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

🚀 A full-stack URL shortening service built with Spring Boot (Java) and deployed on AWS EC2 using Nginx as a reverse proxy and MySQL (RDS) for persistence. Convert long URLs into short, manageable aliases under the custom `d50.in` domain.

🌐 **Live Demo:** [https://d50.in](https://d50.in)

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

*   **Backend:**
    *   ☕ Java 17+
    *   🌱 Spring Boot 3.x (Web, Data JPA, Validation)
    *   💾 Hibernate (ORM)
    *   📝 Lombok (Boilerplate Reduction)
*   **Frontend:**
    *   뼈 HTML5
    *   🎨 CSS3
    *   💡 Vanilla JavaScript (ES6+, Fetch API)
*   **Database:**
    *   🧪 H2 (In-Memory for Local Development/Testing)
    *   🐬 MySQL (Production via AWS RDS)
*   **Proxy / SSL:**
    *   🔒 Nginx (Reverse Proxy, Load Balancing - if scaled, SSL Termination, HTTP->HTTPS Redirect)
    *   🛡️ Let's Encrypt (via Certbot for Free SSL Certificates)
*   **Deployment:**
    *   ☁️ AWS EC2 (Compute Instance)
    *   ☁️ AWS RDS (Managed MySQL Database)
    *   ☁️ AWS Route 53 (DNS Management)
    *   🧱 AWS Security Groups (Firewall)
*   **Build Tool:**
    *   🐘 Maven

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

*   `id` (PK, BIGINT, Auto Increment): Unique record identifier.
*   `alias` (UNIQUE, VARCHAR(10), Not Null): The short alias string.
*   `url` (VARCHAR(2048), Not Null): The original long URL.
*   `created_at` (TIMESTAMP, Not Null): Timestamp of record creation.

*(Diagram Recommendation: Insert a simple ER Diagram visual here)*

---

## 🚀 Getting Started (Local Development)

Follow these steps to run the application locally:

1.  **Prerequisites:**
    *   ☕ JDK 17 or higher installed.
    *   🐘 Maven or Gradle installed.
    *   💾 Optional: Local MySQL server (or use the default H2 in-memory DB).
2.  **Clone the Repository:**
    ```bash
    git clone https://github.com/YOUR_USERNAME/YOUR_REPOSITORY_NAME.git
    cd YOUR_REPOSITORY_NAME
    ```
3.  **Configure for Local:**
    *   Review `src/main/resources/application.properties`.
    *   By default, it should be configured to use H2 for easy local startup.
    *   If using local MySQL, uncomment/add MySQL properties and update credentials:
        ```properties
        #spring.datasource.url=jdbc:mysql://localhost:3306/your_local_db_name?useSSL=false&allowPublicKeyRetrieval=true
        #spring.datasource.username=your_local_db_user
        #spring.datasource.password=your_local_db_password
        #spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
        #spring.jpa.hibernate.ddl-auto=update
        ```
    *   Ensure `app.base-url` is set for local testing (e.g., `app.base-url=http://localhost:8080`).
4.  **Build the Project:**
    *   Using Maven: `mvn clean package`
    *   Using Gradle: `gradle clean build`
5.  **Run the Application:**
    *   Using Maven: `mvn spring-boot:run`
    *   Using Gradle: `gradle bootRun`
    *   Or run the JAR directly: `java -jar target/your-app-name-*.jar` (adjust path if needed)
6.  **Access:** Open your browser and navigate to `http://localhost:8080`.

---

## ☁️ Deployment (AWS EC2 + Nginx)

This outlines the steps for the production deployment used for `d50.in`:

1.  **AWS Prerequisites:**
    *   AWS Account.
    *   Registered Domain Name (`d50.in`) configured in Route 53.
    *   An Elastic IP address associated with your EC2 instance.
2.  **Provision Infrastructure:**
    *   Launch an **EC2 instance** (e.g., Amazon Linux 2 or Ubuntu).
    *   Create an **RDS MySQL instance**.
3.  **Configure Security Groups:**
    *   **EC2 SG:** Allow inbound TCP ports 22 (SSH from Your IP), 80 (HTTP from Anywhere), 443 (HTTPS from Anywhere). **Block port 8080 from Anywhere.**
    *   **RDS SG:** Allow inbound TCP port 3306 *only* from the EC2 instance's Security Group ID.
4.  **Install Software on EC2:**
    *   Connect via SSH.
    *   Install Java (e.g., `sudo amazon-linux-extras install java-openjdk17` or equivalent).
    *   Install Nginx (e.g., `sudo amazon-linux-extras install nginx1`).
    *   Install Certbot (e.g., `sudo yum install certbot python3-certbot-nginx`).
5.  **Obtain SSL Certificate:**
    *   Run `sudo certbot --nginx -d d50.in -d www.d50.in ...` (follow prompts, choose redirect option).
6.  **Configure Nginx:**
    *   Create/edit `/etc/nginx/conf.d/d50.conf` (see synopsis or previous answers for the full config).
    *   Ensure it includes:
        *   Server block for port 80 redirecting to HTTPS.
        *   Server block for port 443 handling SSL (using Certbot paths).
        *   `location / { proxy_pass http://127.0.0.1:8080; ... }` block within the HTTPS server.
        *   Correct `proxy_set_header` directives (Host, X-Real-IP, X-Forwarded-For, X-Forwarded-Proto).
    *   Test (`sudo nginx -t`) and Reload (`sudo systemctl reload nginx`).
7.  **Configure DNS:**
    *   In Route 53, ensure an 'A' record for `d50.in` (and `www`) points to the EC2 instance's Elastic IP.
8.  **Package Application:**
    *   Build the executable JAR: `mvn clean package` or `gradle clean bootJar`.
9.  **Deploy Application:**
    *   Copy the JAR file to the EC2 instance (e.g., using `scp`).
10. **Configure Spring Boot (Production):**
    *   Set **Environment Variables** before running the JAR (or use a systemd service file):
        *   `SERVER_PORT=8080`
        *   `APP_BASE_URL=https://d50.in`
        *   `SPRING_DATASOURCE_URL=jdbc:mysql://<your-rds-endpoint>:3306/<your-db-name>...`
        *   `SPRING_DATASOURCE_USERNAME=<your-rds-username>`
        *   `SPRING_DATASOURCE_PASSWORD=<your-rds-password>`
        *   `SERVER_FORWARD_HEADERS_STRATEGY=framework` (Crucial!)
        *   `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` (Recommended for Prod after initial setup)
11. **Run Application:**
    *   Start the Spring Boot JAR (e.g., `java -jar your-app.jar &` or preferably run as a systemd service for robustness).

---

## ⚙️ Configuration (Environment Variables / Properties)

Key properties required, especially for deployment:

*   `SERVER_PORT`: Internal port Spring Boot listens on (e.g., `8080`).
*   `APP_BASE_URL`: The public base URL used for generating short links (e.g., `https://d50.in`).
*   `SPRING_DATASOURCE_URL`: JDBC URL for the production MySQL database on RDS.
*   `SPRING_DATASOURCE_USERNAME`: Username for the RDS database.
*   `SPRING_DATASOURCE_PASSWORD`: Password for the RDS database.
*   `SERVER_FORWARD_HEADERS_STRATEGY`: Set to `framework` when behind a reverse proxy like Nginx.
*   `SPRING_JPA_HIBERNATE_DDL_AUTO`: Controls schema generation (`validate` or `none` recommended for production).

---

## 📝 Blog Post

*(This section is a placeholder for a link to or summary of a blog post you might write about this project.)*

**Read more about the development process, challenges faced, and deployment details in my blog post:**

➡️ **[Link to Your Blog Post Here]** (Coming Soon!)

*Briefly describe what the blog post covers, e.g., Deep dive into the Nginx configuration, Steps for setting up RDS, Lessons learned during deployment, etc.*

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an Issue for bugs, feature requests, or improvements.

*(Optional: Add more specific contribution guidelines if desired)*

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details. *(Ensure you add a LICENSE.md file with the MIT License text)*

---
