# Multi-stage build for Spring Boot
FROM registry.access.redhat.com/ubi9/openjdk-21:latest as builder
USER root

# Install build tools
RUN microdnf update -y && \
    microdnf install -y gzip tar && \
    microdnf clean all

WORKDIR /build

# Copy Maven files
COPY pom.xml ./
COPY mvnw ./
COPY .mvn ./.mvn

# Make mvnw executable
RUN chmod +x ./mvnw

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests && \
    cp target/*.jar app.jar

# Production stage
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:latest

# Metadata
LABEL maintainer="testplattformar" \
      version="1.0" \
      description="PT-Log Backend Application"

USER root

# Security updates only
RUN microdnf update -y && \
    microdnf clean all && \
    rm -rf /var/cache/yum /tmp/* /var/tmp/*

# Create app and data directories
RUN mkdir -p /opt/app /opt/app/data && \
    chown -R 1001:0 /opt/app && \
    chmod -R 775 /opt/app

WORKDIR /opt/app

# Copy JAR from builder
COPY --from=builder --chown=1001:0 /build/app.jar ./ptlog.jar

# Set JAR permissions
RUN chmod 440 ptlog.jar

# Switch to non-root user
USER 1001

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/healthcheck || exit 1

# JVM Configuration
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.output.ansi.enabled=always"

# Volume for H2 database persistence
VOLUME ["/opt/app/data"]

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar ptlog.jar"]