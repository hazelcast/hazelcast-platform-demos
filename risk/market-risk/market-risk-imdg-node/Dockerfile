FROM library/openjdk:11-jre-slim

ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

ENV JAVA_MODS "--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

ENV JAVA_ARGS ""

# No default for namespace
ENTRYPOINT exec java $JAVA_MODS $JAVA_ARGS -jar application.jar