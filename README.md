# ClockMonster

ClockMonster is a self-hosted service for scheduling one-time or repeating jobs within your system.
Jobs for now are HTTP POST requests, however in the future will be adding to queues, calling gRPC and more.

This service solves the issue of repeating implementations per-service for time-based events. A simple example
of where ClockMonster can come in useful is that your application allows users to create a schedule of times,
and you want to get a call to a service when one of the schedule periods start / end.

Note however, ClockMonster is not per-second accurate. You can configure how often you'd like ClockMonster to
poll for tasks via the environment variable `EXECUTOR_WAIT_SECONDS`.

ClockMonster is built to be horizontally scaled if required. However, this service is very lightweight, so unless
you are creating a ton of tasks, replication shouldn't be required.

Currently, ClockMonster stores its job data in Postgres. You must provide valid a Postgres database configuration
via the environment variables. Eventually, ClockMonster will be able to have multiple data storage methods,
so use-case can be best suited. For example, if you're not too worried about job persistence, ClockMonster could
run just from Redis, or even an internal memory store.

## API documentation
Coming soon. Currently there is a work-in-progress TypeScript API in this repository, but it's not finished yet.\
ClockMonster has REST API endpoints to interact with it. There will be documentation for these too soon, so you can
implement ClockMonster into a different language, or directly into your system.

## How to run ClockMonster

Get it from Docker Hub:
https://hub.docker.com/r/hiett/clockmonster

`docker pull hiett/clockmonster`

Environment variables:\
`DB_HOST`\
`DB_PORT`\
`DB_USERNAME`\
`DB_PASSWORD`\
`DB_DATABASE`\
`DB_MIGRATION_TABLE` (optional)\
`EXECUTOR_WAIT_SECONDS` (default 5)

## Developing ClockMonster

To spin up a Postgres instance for testing with this service:\
`docker run --name clockmonster-postgres -p 5432:5432 -it -e POSTGRES_PASSWORD=clocks -d postgres`

To build docker image from this dir:
```
cd clockmonster
./mvnw package -Pnative -Dquarkus.native.container-build=true
docker build -f src/main/docker/Dockerfile.native-distroless -t hiett/clockmonster .
```
Make sure to have GraalVM installed with native-image from gu.
