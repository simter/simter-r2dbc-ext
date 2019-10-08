# simter-r2dbc-ext changelog

## 1.4.0-M1 - 2019-10-08

- Upgrade to simter-dependencies-1.3.0-M2 (r2dbc-0.8.0.RC2)

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