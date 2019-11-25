FROM openjdk:13-slim

RUN apt-get update && apt-get install -y git

COPY src/QubooScriptPlugin.java /usr/src/
WORKDIR /usr/src/
RUN { echo -n '#!/usr/java/openjdk-13/bin/java --source 13 \n'; cat QubooScriptPlugin.java; } > quboo && \
        chmod +x quboo && \
        mv quboo /usr/local/bin
