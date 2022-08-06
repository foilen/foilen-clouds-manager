FROM openjdk:11-jre-slim

# Install AZ CLI
RUN export TERM=dumb ; \
  apt-get update && apt-get install -y ca-certificates curl apt-transport-https lsb-release gnupg && \
  curl -sL https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor > /etc/apt/trusted.gpg.d/microsoft.gpg && \
  echo "deb [arch=amd64] https://packages.microsoft.com/repos/azure-cli/ $(lsb_release -cs) main" > /etc/apt/sources.list.d/azure-cli.list && \
  apt-get update && apt-get install -y azure-cli \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy the app
COPY app/ /app

ENTRYPOINT ["java", "-jar", "/app/foilen-clouds-manager.jar"]
