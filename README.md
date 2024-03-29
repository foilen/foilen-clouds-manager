# About

A CLI to help managing the infrastructure.

# Quick testing

```
./gradlew bootJar && java -jar build/libs/foilen-clouds-manager-master-SNAPSHOT-boot.jar
```

# Local testing

In interactive mode:
```
./create-local-release.sh

docker run -ti \
  --rm \
  --volume /home:/home \
  --workdir $(pwd) \
  --user $(id -u) \
  --env HOME=$HOME \
  foilen-clouds-manager:main-SNAPSHOT
```

With command:
```
./create-local-release.sh

docker run -ti \
  --rm \
  --volume /home:/home \
  --workdir $(pwd) \
  --user $(id -u) \
  --env HOME=$HOME \
  foilen-clouds-manager:main-SNAPSHOT dns-query \
    --hostname foilen.com
```

# Start it with Docker

In interactive mode:
```
docker run -ti \
  --rm \
  --volume /home:/home \
  --workdir $(pwd) \
  --user $(id -u) \
  --env HOME=$HOME \
  foilen/foilen-clouds-manager
```

With command:
```
docker run -ti \
  --rm \
  --volume /home:/home \
  --workdir $(pwd) \
  --user $(id -u) \
  --env HOME=$HOME \
  foilen/foilen-clouds-manager dns-query \
    --hostname foilen.com
```

If you want to ensure it will use the credential you got with `az login`, you can force its usage:
```
docker run -ti \
  --rm \
  --volume /home:/home \
  --workdir $(pwd) \
  --user $(id -u) \
  --env HOME=$HOME \
  --env FORCE_AZ_CLI_AUTH=true \
  foilen/foilen-clouds-manager azure-dns-zone-list
```

If you want to use a service principal
```
docker run -ti \
  --rm \
  --volume /home:/home \
  --workdir $(pwd) \
  --user $(id -u) \
  --env HOME=$HOME \
  --env AZURE_CLIENT_ID=xxxxxx \
  --env AZURE_CLIENT_SECRET=xxxxxx \
  --env AZURE_TENANT_ID=xxxxxx \
  --env AZURE_SUBSCRIPTION_ID=xxxxxx \
  foilen/foilen-clouds-manager azure-dns-zone-list
```

# To update the IP address of a DNS entry based on the current IP

```
docker run -d --restart always  \
  --rm \
  --volume /home:/home \
  --workdir $(pwd) \
  --user $(id -u) \
  --env HOME=$HOME \
  --env FORCE_AZ_CLI_AUTH=true \
  foilen/foilen-clouds-manager azure-dns-zone-entry-update \
    --resource-group-name my_rg \
    --dns-zone-name foilen.com \
    --hostname server.foilen.com \
    --keep-alive
```

# All available environments

- FORCE_AZ_CLI_AUTH: If set to true, will force the usage of the Azure CLI credentials. Default to false.
- Use an Azure Service Principal:
  - AZURE_CLIENT_ID: The Azure Client ID.
  - AZURE_CLIENT_SECRET: The Azure Client Secret.
  - AZURE_TENANT_ID: The Azure Tenant ID.
  - AZURE_SUBSCRIPTION_ID: The Azure Subscription ID.
- DISABLE_AZURE: If set to true, will disable the Azure commands. Default to false.
- DIGITALOCEAN_TOKEN: The Digital Ocean token.
