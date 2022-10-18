package sbuetter.demo.model

import java.util.UUID

data class Account(
    val id: Id,
    val name: String,
    val customerId: Customer.Id,
    val balance: MonetaryAmount
) {
    @JvmInline
    value class Id(val value: UUID)
}

data class AccountWithTransactions(
    val account: Account,
    val transactions: List<Transaction>
)
