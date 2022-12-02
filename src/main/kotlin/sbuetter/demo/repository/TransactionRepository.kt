package sbuetter.demo.repository

import kotlinx.coroutines.reactive.awaitSingle
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import sbuetter.demo.model.Account
import sbuetter.demo.model.MonetaryAmount
import sbuetter.demo.model.Transaction
import sbuettner.demo.db.tables.Transactions.Companion.TRANSACTIONS
import sbuettner.demo.db.tables.records.TransactionsRecord

@Component
class TransactionRepository {

    context(DSLContext) suspend fun save(transaction: Transaction) {
        insertInto(TRANSACTIONS).set(TRANSACTIONS.ID, transaction.id.value)
            .set(TRANSACTIONS.FROM_ACCOUNT_ID, transaction.accountId.value).set(
                TRANSACTIONS.TO_ACCOUNT_ID,
                when (transaction) {
                    is Transaction.Transfer -> transaction.toAccountId.value
                    else -> null
                }
            ).set(TRANSACTIONS.AMOUNT, transaction.amount.value).set(TRANSACTIONS.TYPE, transaction.type()).returning()
            .awaitSingle().toTransaction()
    }

    fun Transaction.type() = when (this) {
        is Transaction.Deposit -> "deposit"
        is Transaction.Withdrawal -> "withdrawal"
        is Transaction.Transfer -> "transfer"
    }
}

fun TransactionsRecord.toTransaction(): Transaction = when (type) {
    "deposit" -> Transaction.Deposit(Transaction.Id(id!!), MonetaryAmount(amount!!), Account.Id(fromAccountId!!))
    "withdrawal" -> Transaction.Withdrawal(Transaction.Id(id!!), MonetaryAmount(amount!!), Account.Id(fromAccountId!!))
    "transfer" -> Transaction.Transfer(
        Transaction.Id(id!!),
        MonetaryAmount(amount!!),
        fromAccountId = Account.Id(fromAccountId!!),
        toAccountId = Account.Id(toAccountId!!)
    )

    else -> throw IllegalArgumentException("Transaction.type \"$type\" is unknown. ")
}
