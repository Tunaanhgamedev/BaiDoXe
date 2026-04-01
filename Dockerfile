# Stage 1: Build ứng dụng bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Chạy ứng dụng (Dùng bản JRE của eclipse-temurin cho nhẹ)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy file .jar từ thư mục target của stage build sang
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]