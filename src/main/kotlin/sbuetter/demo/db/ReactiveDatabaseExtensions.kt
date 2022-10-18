package sbuetter.demo.db

import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.mono
import org.jooq.DSLContext
import org.jooq.Publisher
import reactor.core.publisher.Flux

suspend fun <T> DSLContext.transactional(block: suspend DSLContext.() -> T): T {
    return transactionPublisher { tx ->
        mono {
            block(tx.dsl())
        }
    }.awaitLast()
}

fun <T> Publisher<T>.toFlow() = Flux.from(this).asFlow()
