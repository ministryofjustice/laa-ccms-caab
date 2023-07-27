FROM eclipse-temurin:17
COPY build/libs/laa-ccms-caab-0.0.1-SNAPSHOT.jar laa-ccms-caab.jar
RUN addgroup --system --gid 800 customgroup \
    && adduser --system --uid 800 --ingroup customgroup --shell /bin/sh customuser
USER 800
EXPOSE 8080

ENV TZ=Europe/London
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=50.0 -XX:MaxRAMPercentage=80.0"
CMD java -jar /laa-ccms-caab.jar
