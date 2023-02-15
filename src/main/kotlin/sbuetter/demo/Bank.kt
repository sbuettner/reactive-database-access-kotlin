package sbuetter.demo

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.springframework.stereotype.Service
import sbuetter.demo.db.DbSupport
import sbuetter.demo.model.Account
import sbuetter.demo.model.Customer
import sbuetter.demo.model.Error
import sbuetter.demo.model.MonetaryAmount
import sbuetter.demo.model.Transaction
import sbuetter.demo.repository.AccountRepository
import sbuetter.demo.repository.CustomerRepository
import sbuetter.demo.repository.TransactionRepository
import java.util.UUID

@Service
class Bank(
    private val customerRepository: CustomerRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val dbSupport: DbSupport
) {

    suspend fun clean() = dbSupport.eitherTx({
        customerRepository.deleteAll()
    }, { Error.Database(it) })

    suspend fun createCustomer(name: String) = dbSupport.eitherTx({
        val customer = Customer(
            id = Customer.Id(UUID.randomUUID()),
            name = name
        )
        customerRepository.save(customer)
    }, { e ->
        when {
            e.isConstraintException("customers_name_key") -> Error.CreateCustomer.CustomerNameAlreadyExists(name)
            else -> Error.Database(e)
        }
    })

    suspend fun openAccount(customerId: Customer.Id, name: String) = dbSupport.eitherTx({
        val account = Account(
            id = Account.Id(UUID.randomUUID()),
            customerId = customerId,
            name = name,
            balance = MonetaryAmount(0)
        )
        accountRepository.save(account)
    }, { e: Throwable ->
        when {
            e.isConstraintException("accounts_customer_id_fkey") -> Error.OpenAccount.CustomerNotFound(customerId)
            else -> Error.Database(e)
        }
    })

    private fun Throwable.isConstraintException(constraintName: String) =
        this is DataAccessException && cause?.message?.contains(constraintName) == true

    suspend fun deposit(accountId: Account.Id, amount: MonetaryAmount) = dbSupport.eitherTx<Error.Deposit, Unit>({
        val deposit = Transaction.Deposit(
            id = Transaction.Id(UUID.randomUUID()),
            accountId = accountId,
            amount = amount
        )
        deposit.accountId.add(deposit.amount.value).bind()
        transactionRepository.save(deposit)
    }, { Error.Database(it) })

    suspend fun withdraw(accountId: Account.Id, amount: MonetaryAmount) = dbSupport.eitherTx<Error.Withdrawal, Unit>({
        val withdrawal = Transaction.Withdrawal(
            id = Transaction.Id(UUID.randomUUID()),
            accountId = accountId,
            amount = amount
        )
        withdrawal.accountId.add(-withdrawal.amount.value).bind()
        transactionRepository.save(withdrawal)
    }, { Error.Database(it) })

    suspend fun transfer(
        fromAccountId: Account.Id,
        toAccountId: Account.Id,
        amount: MonetaryAmount
    ) = dbSupport.eitherTx<Error.Transfer, Unit>({
        val transfer = Transaction.Transfer(
            id = Transaction.Id(UUID.randomUUID()),
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            amount = amount
        )
        transfer.toAccountId.add(transfer.amount.value).bind()
        transfer.fromAccountId.add(-transfer.amount.value).bind()
        transactionRepository.save(transfer)
    }, { Error.Database(it) })

    context (DSLContext)
    private suspend fun Account.Id.add(amount: Int): Either<Error.Transaction, Unit> =
        accountRepository.add(this, amount).let { newBalance ->
            when {
                newBalance == null -> Error.Transaction.AccountNotFound(this).left()
                newBalance < 0 -> Error.Transaction.CreditLineExceeded(newBalance).left()
                else -> Unit.right()
            }
        }

    suspend fun findAccountsWithTransactions(customerId: Customer.Id) = dbSupport.eitherTx({
        accountRepository.fetchAccountsWithTransactions(customerId)
    }, { it })
}
