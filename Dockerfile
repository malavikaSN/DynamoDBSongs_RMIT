FROM maven:3.9-eclipse-temurin-17

WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY 2026a2_songs.json .

RUN mvn -q -DskipTests package

ENV PORT=80
EXPOSE 80

CMD ["mvn", "exec:java", "-Dexec.mainClass=com.amazonaws.samples.ApiServer"]