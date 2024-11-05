FROM --platform=linux/amd64 bitnami/kafka:3.8.0

USER root
RUN apt upgrade
RUN apt update
RUN apt-get install -y wget
RUN cd /tmp ; \
    wget -q https://repo.maven.apache.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/1.0.1/jmx_prometheus_javaagent-1.0.1.jar ; \
    ls ;
	
COPY target/classes/custom-entrypoint.sh  /
COPY target/classes/prometheus.yml /

ENV JAVA_AGENT_CONFIG=/prometheus.yml
ENV JAVA_AGENT_JAR=/tmp/jmx_prometheus_javaagent-1.0.1.jar
ENV JAVA_AGENT_PORT=7071

CMD ["bash", "-c", "set -euo pipefail \
&& echo @@@@@@@@@@ \
&& echo ls -l /tmp \
&& ls -l /tmp \
&& echo @@@@@@@@@@ \
&& export JAVA_TOOL_OPTIONS=-javaagent:${JAVA_AGENT_JAR}=${JAVA_AGENT_PORT}:${JAVA_AGENT_CONFIG} \
&& echo JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS} \
&& echo @@@@@@@@@@ \
&& chmod 755 /custom-entrypoint.sh \
&& echo /custom-entrypoint.sh \
&& /custom-entrypoint.sh \
"]
