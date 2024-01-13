
FROM openjdk:17

WORKDIR /app

COPY build/libs/manicure-bot-0.0.1-SNAPSHOT-plain.jar /app/manicure-bot.jar

CMD ["java", "-jar", "manicure-bot.jar"]
