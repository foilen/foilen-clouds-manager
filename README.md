# About

A CLI to help managing the infrastructure.

# Quick testing

```
./gradlew bootJar && java -jar build/libs/foilen-clouds-manager-master-SNAPSHOT-boot.jar
```

# Local testing

```
./create-local-release.sh

docker run -ti \
  --rm \
  --volume /home:/home \
  --workdir $(pwd) \
  --user $(id -u) \
  --env HOME=$HOME \
  foilen-clouds-manager:master-SNAPSHOT
```

# Start it

```
docker run -ti \
  --rm \
  --volume /home:/home \
  --workdir $(pwd) \
  --user $(id -u) \
  --env HOME=$HOME \
  foilen/foilen-clouds-manager
```
