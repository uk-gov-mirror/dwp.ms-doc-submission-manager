FROM gcr.io/distroless/java17-debian12:nonroot@sha256:8dd4f872897b75d1f2c45b630ef0bbbbeba57bc7da37d68726ac66dc801595c6
COPY --chown=nonroot:nonroot target/ms-document-submission-manager-*.jar /app.jar

COPY --from=pik94420.live.dynatrace.com/linux/oneagent-codemodules:java / /
ENV LD_PRELOAD /opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so

EXPOSE 8080

USER nonroot
ENTRYPOINT ["java", "-jar", "/app.jar"]
