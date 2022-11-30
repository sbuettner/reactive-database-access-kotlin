package sbuetter.demo.db

import arrow.core.Either
import arrow.core.Either.Companion.catch
import io.r2dbc.spi.Connection
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.jooq.DSLContext
import org.jooq.Publisher
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class DbSupport(val databaseClient: DatabaseClient) {

    private val settings = Settings().withBindOffsetDateTimeType(true).withBindOffsetTimeType(true)

    private fun Connection.dsl() = DSL.using(this, SQLDialect.POSTGRES, settings)

    suspend fun <T> catchTx(block: suspend DSLContext.() -> T) = catch {
        databaseClient.inConnection { con ->
            mono { block.invoke(con.dsl()) }
        }.awaitSingle()
    }

    suspend fun <A, B> eitherTx(block: suspend DSLContext.() -> Either<A, B>) = catch {
        databaseClient.inConnection { con ->
            mono {
                con.beginTransaction().awaitFirstOrNull()
                block(con.dsl()).map { r ->
                    con.commitTransaction().awaitFirstOrNull()
                    r
                }.mapLeft { e ->
                    con.rollbackTransaction().awaitFirstOrNull()
                    e
                }
            }
        }.awaitLast()
    }
}

fun <T> Publisher<T>.toFlow() = Flux.from(this).asFlow()
