# Giai đoạn 1: Build mã nguồn & tối ưu cache thư viện
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Tải trước thư viện (chỉ chạy lại khi đổi pom.xml)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy code và đóng gói
COPY src ./src
RUN mvn clean package -DskipTests

# Giai đoạn 2: Môi trường chạy gọn nhẹ
FROM openjdk:17.0.1-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]