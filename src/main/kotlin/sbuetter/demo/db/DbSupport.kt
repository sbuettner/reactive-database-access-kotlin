package sbuetter.demo.db

import arrow.core.Either
import arrow.core.Either.Companion.catch
import arrow.core.continuations.EffectScope
import arrow.core.continuations.effect
import arrow.core.flatten
import io.r2dbc.spi.Connection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component

@Component
class DbSupport(val databaseClient: DatabaseClient) {

    private val settings = Settings().withBindOffsetDateTimeType(true).withBindOffsetTimeType(true)

    private fun Connection.dsl() = DSL.using(this, SQLDialect.POSTGRES, settings)

    suspend fun <E, A> eitherTx(
        block: suspend EffectScope<E>.(DSLContext) -> A,
        mapLeftHandler: (Throwable) -> E
    ): Either<E, A> = catch {
        databaseClient.inConnection { con ->
            mono {
                con.beginTransaction().awaitFirstOrNull()
                effect { block(this, con.dsl()) }.toEither().map {
                    con.commitTransaction().awaitFirstOrNull()
                    it
                }.mapLeft {
                    con.rollbackTransaction().awaitFirstOrNull()
                    it
                }
            }
        }.awaitSingle()
    }.mapLeft(mapLeftHandler).flatten()
}
