export SERVER_PORT=8080
export APP_BASE_URL=https://d50.in
export SPRING_DATASOURCE_URL='jdbc:mysql://url-short-db.cbymmwois1cu.ap-south-1.rds.amazonaws.com:3306/urlshortener?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export SPRING_DATASOURCE_USERNAME=admin
export SPRING_DATASOURCE_PASSWORD=mysql123
export SERVER_FORWARD_HEADERS_STRATEGY=framework


Using screen:
Install screen if not present (e.g., sudo apt install screen or sudo yum install screen).
Start a new screen session:
  screen -S myappsession (Replace myappsession with a name you like)
You are now inside the screen session. Run your Spring Boot app as usual:
  java -jar /path/to/your/spring-app.jar
Detach from the screen session: Press Ctrl+A, then D (press Ctrl and A together, release, then press D).
You are back in your original PuTTY terminal. Your application continues running inside the detached screen session. You can now close PuTTY.
To reattach later:
Open a new PuTTY session and type:
screen -r myappsession
(If you have multiple sessions, screen -ls lists them, then use screen -r <session_id_or_name>)
