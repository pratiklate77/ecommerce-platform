# syntax=docker/dockerfile:1
# -----------------------------------------------------------------------------
# Service: api-gateway (Spring Boot 4.1.1 / Java 17)
#
# Multi-stage, production-ready build:
#   STAGE 1 : Maven build with a dedicated dependency layer for cache reuse.
#   STAGE 2 : Slim JRE runtime, non-root user, minimal surface.
#
# Build context must be THIS service directory:
#   docker build -t ecommerce/product-service ./backend/product-service
# -----------------------------------------------------------------------------

# ------------------------- Build stage ---------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# 1) Resolve dependencies + plugins BEFORE copying source.
#    This makes Maven dependency layers cached: a source-only change re-runs
#    only the final package step, not the (slow) dependency resolution.
COPY pom.xml ./
RUN mvn -B -ntp -DskipTests dependency:go-offline

# 2) Copy source and build the (non-tagged) production jar.
COPY src ./src
RUN mvn -B -ntp -DskipTests clean package

# ------------------------- Runtime stage -------------------------------------
FROM eclipse-temurin:17-jre-jammy

# Java runtime tuning, overridable at deploy time (e.g. -Xmx).
ENV JAVA_OPTS="" \
    TZ="UTC" \
    SERVER_PORT=8080

# Non-root user. 10001 is an arbitrarily high, stable UID/GID.
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /sbin/nologin app

WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar

# Drop privileges before running.
USER app

EXPOSE 8080

# The jar has an embedded main-class manifest; no classpath juggling needed.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]