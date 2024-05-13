FROM eclipse-temurin:21
VOLUME /tmp
COPY laa-ccms-caab-0.0.1-SNAPSHOT.jar laa-ccms-caab.jar
EXPOSE 8080
RUN addgroup --system --gid 800 customgroup \
    && adduser --system --uid 800 --ingroup customgroup --shell /bin/sh customuser
RUN chown customuser:customgroup laa-ccms-caab.jar
USER 800

RUN ls -l laa-ccms-caab.jar
ENV TZ=Europe/London
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=50.0 -XX:MaxRAMPercentage=80.0"
CMD java -jar laa-ccms-caab.jar
