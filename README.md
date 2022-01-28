# ClockMonster

Get it from Docker Hub:
https://hub.docker.com/r/hiett/clockmonster
`docker pull hiett/clockmonster`

Environment variables:
`DB_HOST`
`DB_PORT`
`DB_USERNAME`
`DB_PASSWORD`
`DB_DATABASE`
`DB_MIGRATION_TABLE` (optional)
`EXECUTOR_WAIT_SECONDS` (default 5)

To build docker image from this dir:
```
cd clockmonster
./mvnw package -Pnative -Dquarkus.native.container-build=true
docker build -f src/main/docker/Dockerfile.native-distroless -t hiett/clockmonster .
```
Make sure to have GraalVM installed with native-image from gu.

To spin up a Postgres instance for testing with this service:
`docker run --name clockmonster-postgres -p 5432:5432 -it -e POSTGRES_PASSWORD=clocks -d postgres`