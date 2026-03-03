FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY build/libs/commitbody-0.0.1-SNAPSHOT.jar /app/commitbody.jar

ENTRYPOINT ["java", "-jar","commitbody.jar"]