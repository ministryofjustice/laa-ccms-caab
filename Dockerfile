FROM ghcr.io/ministryofjustice/laa-base-container-images/java:25-corretto-alpine

# PDF rendering uses AWT font support, which needs fontconfig and FreeType on Alpine.
USER root
RUN apk add --no-cache fontconfig ttf-dejavu
USER 1001:1001

COPY build/libs/laa-ccms-caab-*.jar /app/laa-ccms-caab.jar

EXPOSE 8080
CMD ["java", "-jar", "/app/laa-ccms-caab.jar"]
