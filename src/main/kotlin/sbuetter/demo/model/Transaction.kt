package sbuetter.demo.model

import java.util.UUID

sealed class Transaction(
    open val id: Id,
    open val amount: MonetaryAmount,
    open val accountId: Account.Id
) {
    @JvmInline
    value class Id(val value: UUID)

    data class Deposit(
        override val id: Id,
        override val amount: MonetaryAmount,
        override val accountId: Account.Id
    ) : Transaction(id, amount, accountId)

    data class Withdrawal(
        override val id: Id,
        override val amount: MonetaryAmount,
        override val accountId: Account.Id
    ) : Transaction(id, amount, accountId)

    data class Transfer(
        override val id: Id,
        override val amount: MonetaryAmount,
        val fromAccountId: Account.Id,
        val toAccountId: Account.Id
    ) : Transaction(id, amount, fromAccountId)
}
