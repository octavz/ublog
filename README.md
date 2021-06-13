# Micro blog app using ZIO

This application is a POC for ZIO capabilities.

For more information, check the [ZIO documentation](https://zio.dev/docs/overview/overview_index)

The application is divided in several modules, represented by ZIO layers:

- web layer - uses [AkkaHttp](https://doc.akka.io/docs/akka-http/current/index.html)
- data layer - uses cache and persistence layers
- persistence layer - uses [Doobie](https://tpolecat.github.io/doobie/) as a functional library for JDBC and
  [tranzaction](https://github.com/gaelrenoux/tranzactio) as ZIO wrapper for Doobie
- cache layer - uses Redis and [redis4cats](https://github.com/profunktor/redis4cats)

At load time, [Flyway](https://flywaydb.org/documentation/) starts the database provisioning, then the AkkaHttp layer
runs the ZIO programs from the logic layer.