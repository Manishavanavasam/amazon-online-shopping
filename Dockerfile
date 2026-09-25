# Use lightweight OpenJDK runtime image
FROM eclipse-temurin:21-jre-alpine AS runner

# Set working directory
WORKDIR /app

# Install JDK tools for compilation stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy source code
COPY . .

# Compile all Java sources
RUN javac Customer.java Product.java CartItem.java AmazonWebServer.java AmazonShopping.java

# Build executable JAR
RUN jar cfe app.jar AmazonWebServer *.class

# Final minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy compiled JAR and persistent directory from builder
COPY --from=builder /build/app.jar app.jar

# Expose default port
EXPOSE 8080

# Environment port fallback
ENV PORT=8080

# Run the web application
CMD ["java", "-jar", "app.jar"]
