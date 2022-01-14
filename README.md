# ClockMonster

Get it from Docker Hub:
https://hub.docker.com/r/hiett/clockmonster
`docker pull hiett/clockmonster`

Environment variables:
`DB_HOST`
`DB_PORT`
`DB_USERNAME`
`DB_PASSWORD`
`DB_MIGRATION_TABLE` (optional)

To build docker image from this dir:
```
cd clockmonster
./mvnw package -Pnative -Dquarkus.native.container-build=true
docker build -f src/main/docker/Dockerfile.native-distroless -t hiett/clockmonster .
```
Make sure to have GraalVM installed with native-image from gu.