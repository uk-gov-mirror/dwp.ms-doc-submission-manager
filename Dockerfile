FROM registry.gitlab.com/dwp/secure-development/registry-mirrors/chainguard/jre:openjdk-21.0.11
COPY --chown=nonroot:nonroot target/ms-document-submission-manager-*.jar /app.jar

COPY --from=eyq18885.live.dynatrace.com/linux/oneagent-codemodules:java / /
ENV LD_PRELOAD /opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so

EXPOSE 8080

USER java
ENTRYPOINT ["java", "-jar", "/app.jar"]
