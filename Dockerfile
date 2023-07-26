FROM openjdk:17
USER root
VOLUME /tmp
RUN ls -l home
COPY build/libs/laa-ccms-caab-0.0.1-SNAPSHOT.jar laa-ccms-caab.jar
EXPOSE 8080

ENV TZ=Europe/London
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=50.0 -XX:MaxRAMPercentage=80.0"
CMD java -jar /laa-ccms-caab.jar
