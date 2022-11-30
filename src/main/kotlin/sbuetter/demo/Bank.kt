package sbuetter.demo

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.flatten
import arrow.core.left
import arrow.core.right
import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.springframework.stereotype.Service
import sbuetter.demo.db.AccountRepository
import sbuetter.demo.db.CustomerRepository
import sbuetter.demo.db.DbSupport
import sbuetter.demo.db.TransactionRepository
import sbuetter.demo.model.Account
import sbuetter.demo.model.Customer
import sbuetter.demo.model.MonetaryAmount
import sbuetter.demo.model.Transaction
import java.util.UUID

@Service
class Bank(
    private val customerRepository: CustomerRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val dbSupport: DbSupport
) {

    suspend fun clean(): Either<Error.Database, Int> = dbSupport.catchTx {
        customerRepository.deleteAll()
    }.mapLeft { Error.Database(it) }

    suspend fun createCustomer(name: String) = dbSupport.catchTx {
        val customer = Customer(
            id = Customer.Id(UUID.randomUUID()), name = name
        )
        customerRepository.save(customer)
    }.mapLeft { e ->
        when {
            e.isConstraintException("customers_name_key") -> Error.CreateCustomer.CustomerNameAlreadyExists(name)
            else -> Error.Database(e)
        }
    }

    suspend fun openAccount(customerId: Customer.Id, name: String): Either<Error.OpenAccount, Account> =
        dbSupport.catchTx {
            val account = Account(
                id = Account.Id(UUID.randomUUID()), customerId = customerId, name = name, balance = MonetaryAmount(0)
            )
            accountRepository.save(account)
        }.mapLeft { e: Throwable ->
            when {
                e.isConstraintException("accounts_customer_id_fkey") -> Error.OpenAccount.CustomerNotFound(customerId)
                else -> Error.Database(e)
            }
        }

    private fun Throwable.isConstraintException(constraintName: String) =
        this is DataAccessException && cause?.message?.contains(constraintName) == true

    suspend fun deposit(accountId: Account.Id, amount: MonetaryAmount): Either<Error.Deposit, Unit> =
        dbSupport.eitherTx {
            val deposit = Transaction.Deposit(
                id = Transaction.Id(UUID.randomUUID()), accountId = accountId, amount = amount
            )
            either {
                deposit.accountId.add(deposit.amount.value).bind()
                transactionRepository.save(deposit)
            }
        }.mapLeft { Error.Database(it) }.flatten()

    suspend fun withdraw(accountId: Account.Id, amount: MonetaryAmount): Either<Error.Withdrawal, Unit> =
        dbSupport.eitherTx {
            val withdrawal = Transaction.Withdrawal(
                id = Transaction.Id(UUID.randomUUID()), accountId = accountId, amount = amount
            )
            either {
                withdrawal.accountId.add(-withdrawal.amount.value).bind()
                transactionRepository.save(withdrawal)
            }
        }.mapLeft {
            Error.Database(it)
        }.flatten()

    suspend fun transfer(
        fromAccountId: Account.Id, toAccountId: Account.Id, amount: MonetaryAmount
    ): Either<Error.Transfer, Unit> = dbSupport.eitherTx {
        val transfer = Transaction.Transfer(
            id = Transaction.Id(UUID.randomUUID()),
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            amount = amount
        )
        either<Error.Transfer, Unit> {
            transfer.toAccountId.add(transfer.amount.value).bind()
            transfer.fromAccountId.add(-transfer.amount.value).bind()
            transactionRepository.save(transfer)
        }
    }.mapLeft { Error.Database(it) }.flatten()

    context (DSLContext) private suspend fun Account.Id.add(amount: Int): Either<Error.Transaction, Unit> =
        accountRepository.add(this, amount).let { newBalance ->
            when {
                newBalance == null -> Error.Transaction.AccountNotFound(this).left()
                newBalance < 0 -> Error.Transaction.CreditLineExceeded(newBalance).left()
                else -> Unit.right()
            }
        }

    suspend fun findAccountsWithTransactions(customerId: Customer.Id) =
        accountRepository.fetchAccountsWithTransactions(customerId)
}
