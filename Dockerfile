FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/DynamoDBSongs-1.0-SNAPSHOT.jar /app/app.jar
EXPOSE 4567
ENV JAVA_OPTS="-Xmx512m"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
