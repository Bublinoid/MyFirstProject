FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . /app
CMD ["java", "-jar", "build/libs/manicure-bot-0.0.1-SNAPSHOT-plain.jar"]
