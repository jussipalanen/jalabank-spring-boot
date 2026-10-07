# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /home/app
# Download dependencies first so they are cached between builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# This runs the tests too; add -DskipTests to build faster
RUN mvn -B package

# Run
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 jalabank
COPY --from=build /home/app/target/jalabank.jar /usr/local/lib/jalabank.jar
USER jalabank
ENV PORT=8080
EXPOSE 8080
# Keep the heap within the container's memory limit (e.g. 512 MB on small hosting plans)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/usr/local/lib/jalabank.jar"]
