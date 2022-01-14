# ClockMonster

Very WIP, will have lots of infos here when more complete

To run a postgres DB for now: `docker run --name clockmonster-postgres -p 5432:5432 -it -e POSTGRES_PASSWORD=clocks -d postgres`

Environment variables:
`DB_HOST`
`DB_PORT`
`DB_USERNAME`
`DB_PASSWORD`
`DB_MIGRATION_TABLE` (optional)