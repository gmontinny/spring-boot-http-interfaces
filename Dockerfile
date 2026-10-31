# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copia apenas os arquivos de dependência primeiro (cache de camadas)
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q

# Copia o restante do código e compila
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test -q

# ── Stage 2: runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Usuário não-root por segurança
RUN addgroup -S spring && adduser -S spring -G spring

# Copia o JAR do stage anterior
COPY --from=builder /app/build/libs/*.jar app.jar

# Copia os CSVs para importação automática via DataLoader
COPY data/ data/

RUN chown -R spring:spring /app
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
