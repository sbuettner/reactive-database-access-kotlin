package sbuetter.demo

import arrow.core.Either
import sbuetter.demo.model.Account
import sbuetter.demo.model.Customer

sealed interface Error {

    data class Database(val e: Throwable) : CreateCustomer, Transaction, OpenAccount

    sealed interface CreateCustomer : Error {
        data class CustomerNameAlreadyExists(val name: String) : CreateCustomer
    }

    sealed interface OpenAccount : Error {
        data class CustomerNotFound(val customerId: Customer.Id) : OpenAccount
    }

    sealed interface Deposit : Error

    sealed interface Withdrawal : Error

    sealed interface Transfer : Error

    sealed interface Transaction : Withdrawal, Transfer {
        data class AccountNotFound(val accountId: Account.Id) : Transaction, Deposit

        data class CreditLineExceeded(val balanceValue: Int) : Transaction
    }
}

/**
 * Small hack to avoid custom exceptions and mapping for individual errors.
 */
data class ErrorException(val error: Error) : RuntimeException("Error happened: $error")

fun Error.raise() {
    throw ErrorException(this)
}

inline fun <reified L : Error, R> Either<Throwable, R>.handleError(): Either<L, R> = mapLeft { e ->
    when (e) {
        is ErrorException -> e.error
        else -> Error.Database(e)
    } as L
}
