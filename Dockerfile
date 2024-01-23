FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/manicure-bot-0.0.1-SNAPSHOT-plain.jar .
CMD ["java", "-jar", "manicure-bot-0.0.1-SNAPSHOT-plain.jar"]
