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

ClockMonster supports storing data in either Postgres or Redis. In the future, more databases will be added.
You can select the storage method by the environment variable `JOB_STORAGE_METHOD` (see below).

### Storage Method Notes
- Postgres
  - Will create some tables that ClockMonster will manage.
  - To execute, ClockMonster will run a simple query looking for all jobs in the past
- Redis
  - Uses scored sets, where the score is the execution time. Value is the job id. Then utilises `zrangebyscore` to order the jobs that are in the past.
  - Currently, stores the actual job payloads in a different key. The lua script will use `mget` to grab them all at once for jobs that require executing.
  - Due to the separation of jobs in keys and their scores in the set, as much of the grouped Redis logic as possible is within Lua scripts  

## API documentation
Coming soon. Currently there is a work-in-progress TypeScript API in this repository, but it's not finished yet.\
ClockMonster has REST API endpoints to interact with it. There will be documentation for these too soon, so you can
implement ClockMonster into a different language, or directly into your system.

## How to run ClockMonster

Get it from Docker Hub:
https://hub.docker.com/r/hiett/clockmonster

`docker pull hiett/clockmonster`

Environment variables:\
`JOB_STORAGE_METHOD` (must be either `POSTGRES` or `REDIS`)\
`DB_HOST` (DB configuration only required if `JOB_STORAGE_METHOD`=`POSTGRES`)\
`DB_PORT`\
`DB_USERNAME`\
`DB_PASSWORD`\
`DB_DATABASE`\
`DB_MIGRATION_TABLE` (optional)\
`REDIS_CONN_URL` (Redis configuration only required if `JOB_STORAGE_METHOD`=`REDIS`)\
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
