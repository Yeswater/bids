FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /w
COPY backend/pom.xml backend/pom.xml
COPY backend/bids-sql-dialect backend/bids-sql-dialect
COPY backend/bids-exec backend/bids-exec
WORKDIR /w/backend
RUN mvn -q -pl bids-exec -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /w/backend/bids-exec/target/bids-exec-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8082
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
