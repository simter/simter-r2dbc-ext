# simter-r2dbc-ext changelog

## 1.4.0-M5 - 2020-04-15

- Upgrade to simter-dependencies-1.3.0-M14

## 1.4.0-M4 - 2020-03-02

- Release connection after execute initial sql [#1]
- Upgrade to simter-dependencies-1.3.0-M13

[#1]: https://github.com/simter/simter-r2dbc-ext/issues/1

## 1.4.0-M3 - 2020-01-19

- Upgrade to simter-dependencies-1.3.0-M12
- Rename `R2dbcCustomConverter<S, T>` to `SimterR2dbcConverter<S, T>`
- Support bidirectional r2dbc converter by `SimterR2dbcBiConverter<S, T> extends SimterR2dbcConverter<S, T>`

## 1.4.0-M2 - 2019-12-11

- Upgrade to simter-dependencies-1.3.0-M9 (r2dbc-Arabba-RELEASE)
- Support auto register all `R2dbcCustomConverter` beans for r2dbc custom converters
- Add `tech.simter.r2dbc.R2dbcCustomConverter<S, T> extends Converter<S, T>` marker interface
- Remove lombok
- Remove r2dbc-client
- Change to use ['r2dbc ConnectionFactory Discovery Mechanism'] to generate `ConnectionFactory` instance
- Add protocol property to R2dbcProperties for h2
- Add dev.miku:r2dbc-mysql optional dependency

['r2dbc ConnectionFactory Discovery Mechanism']: https://r2dbc.io/spec/0.8.0.RELEASE/spec/html/#connections.factory.discovery

## 1.4.0-M1 - 2019-10-08

- Upgrade to simter-dependencies-1.3.0-M2 (r2dbc-Arabba-RC2)

## 1.3.0 - 2019-07-03

- Change parent to simter-dependencies-1.2.0
- Change init database lifecycle to ContextRefreshedEvent - after all bean initialized
- Add concat-sql-script control by property spring.datasource.concat-sql-script
- Execute spring.datasource.schema|data script one by one
- Set spring milestone version config

## 1.2.0 - 2019-02-15

- Read SQL file with encoding UTF-8
- Add platform and data property to R2dbcProperties
- Save concatenate SQL content to file `target/${db.platform}.sql` if log level is info

## 1.1.1 - 2019-01-13

- Add `@ComponentScan` on `R2dbcConfiguration` class

## 1.1.0 - 2019-01-11

- Convert to java module

## 1.0.0 - 2019-01-08

- Auto config `ConnectionFactory` by dependency with [R2dbcProperties]

[R2dbcProperties]: https://github.com/simter/simter-r2dbc-ext/blob/master/src/main/java/tech/simter/r2dbc/R2dbcProperties.kt