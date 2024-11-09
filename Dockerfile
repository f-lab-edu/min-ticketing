FROM openjdk:17.0.1-jdk-slim

WORKDIR /app

# Setup Pinpoint Agent
ENV PINPOINT_VERSION=2.5.2
ENV AGENT_PATH=/pinpoint-agent

# Download and setup Pinpoint Agent using ADD
ADD https://github.com/pinpoint-apm/pinpoint/releases/download/v${PINPOINT_VERSION}/pinpoint-agent-${PINPOINT_VERSION}.tar.gz /app/

# Create agent directory and extract
RUN mkdir -p ${AGENT_PATH} \
    && tar -xzf pinpoint-agent-${PINPOINT_VERSION}.tar.gz -C ${AGENT_PATH} --strip-components=1 \
    && rm pinpoint-agent-${PINPOINT_VERSION}.tar.gz

# Create logs directory
RUN mkdir -p /app/logs && chmod -R 777 /app/logs

COPY build/libs/ticketing-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
EXPOSE 9090

# Add Pinpoint agent to Java command
ENTRYPOINT ["sh", "-c", "java \
    -javaagent:${AGENT_PATH}/pinpoint-bootstrap-${PINPOINT_VERSION}.jar \
    -Dpinpoint.agentId=${PINPOINT_AGENT_ID} \
    -Dpinpoint.applicationName=${PINPOINT_APPLICATION_NAME} \
    -Dprofiler.collector.ip=${PINPOINT_COLLECTOR_IP} \
    -Dprofiler.transport.grpc.collector.ip=${PINPOINT_COLLECTOR_IP} \
    -Dprofiler.sampling.counting.sampling-rate=${PROFILER_SAMPLING_COUNTING_SAMPLING_RATE} \
    -Dprofiler.sampling.percent.sampling-rate=${PROFILER_SAMPLING_PERCENT_SAMPLING_RATE} \
    -jar /app/app.jar"]