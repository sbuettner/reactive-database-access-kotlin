package sbuetter.demo.db

import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.mono
import org.jooq.DSLContext
import org.jooq.Publisher
import reactor.core.publisher.Flux

suspend fun <T> DSLContext.transactional(block: suspend DSLContext.() -> T): T {
    // Using JOOQs extension uses awaitFirstOrNull which leads to
    // different transactional behaviour: java.lang.IllegalStateException: The connection is closed
    // return transactionCoroutine { config -> block(config.dsl()) }
    return transactionPublisher { tx ->
        mono { block(tx.dsl()) }
    }.awaitLast() as T
}

fun <T> Publisher<T>.toFlow() = Flux.from(this).asFlow()
