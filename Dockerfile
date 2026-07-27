#Etapa 1: Construccion(Builder)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
#Copiar el wrapper de Maven y los archivos de configuracion
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
#dar permisos de ejecucion al wrapper y compilar el proyecto
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests
#Etapa 2: imagen final para produccion (ligera)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
#copiar solo el archivo .jar generado en la etapa anterior
COPY --from=builder /app/target/* .jar app.jar
#exponer el puerto (por defecto spring boot usa 8080)
EXPOSE 8080
#Comando para ejecutar la aplicacion
ENTRYPONT["java", ".jar", "app.jar"]