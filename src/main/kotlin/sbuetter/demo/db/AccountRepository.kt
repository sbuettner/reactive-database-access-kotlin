package sbuetter.demo.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.jooq.DSLContext
import org.jooq.impl.DSL.multiset
import org.jooq.impl.DSL.selectFrom
import org.jooq.kotlin.intoList
import org.springframework.stereotype.Component
import sbuetter.demo.model.Account
import sbuetter.demo.model.AccountWithTransactions
import sbuetter.demo.model.Customer
import sbuetter.demo.model.MonetaryAmount
import sbuetter.demo.model.Transaction
import sbuettner.demo.db.tables.Accounts.Companion.ACCOUNTS
import sbuettner.demo.db.tables.Transactions.Companion.TRANSACTIONS
import sbuettner.demo.db.tables.records.AccountsRecord

@Component
class AccountRepository(val dsl: DSLContext) {

    context (DSLContext) suspend fun save(account: Account) = insertInto(ACCOUNTS)
        .set(ACCOUNTS.ID, account.id.value)
        .set(ACCOUNTS.NAME, account.name)
        .set(ACCOUNTS.CUSTOMER_ID, account.customerId.value)
        .set(ACCOUNTS.BALANCE, account.balance.value)
        .returning()
        .awaitFirst().toAccount()

    @Suppress("UNCHECKED_CAST")
    suspend fun fetchAccountsWithTransactions(customerId: Customer.Id): Flow<AccountWithTransactions> {
        return dsl.select(
            *ACCOUNTS.fields(),
            multiset(
                selectFrom(TRANSACTIONS).where(TRANSACTIONS.FROM_ACCOUNT_ID.eq(ACCOUNTS.ID))
            ).`as`("tx").intoList { it.toTransaction() }
        ).from(ACCOUNTS).where(ACCOUNTS.CUSTOMER_ID.eq(customerId.value))
            .toFlow().map {
                val account = it.into(ACCOUNTS).toAccount()
                val transactions = it["tx"] as List<Transaction>
                AccountWithTransactions(account, transactions)
            }
    }

    fun AccountsRecord.toAccount() = Account(
        id = Account.Id(id!!),
        name = name!!,
        balance = MonetaryAmount(balance!!),
        customerId = Customer.Id(customerId!!)
    )

    context(DSLContext) suspend fun add(accountId: Account.Id, amount: Int): Int? {
        return update(ACCOUNTS).set(ACCOUNTS.BALANCE, ACCOUNTS.BALANCE.plus(amount))
            .where(ACCOUNTS.ID.eq(accountId.value))
            .returning().awaitFirstOrNull()?.balance
    }
}
