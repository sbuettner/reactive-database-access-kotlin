package sbuetter.demo

import arrow.core.Either
import arrow.core.Either.Companion.catch
import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.springframework.stereotype.Service
import sbuetter.demo.db.AccountRepository
import sbuetter.demo.db.CustomerRepository
import sbuetter.demo.db.TransactionRepository
import sbuetter.demo.db.transactional
import sbuetter.demo.model.Account
import sbuetter.demo.model.Customer
import sbuetter.demo.model.MonetaryAmount
import sbuetter.demo.model.Transaction
import java.util.UUID

@Service
class Bank(
    val customerRepository: CustomerRepository,
    val accountRepository: AccountRepository,
    val transactionRepository: TransactionRepository,
    val dslContext: DSLContext
) {

    suspend fun clean(): Either<Error.Database, Int> = catch {
        dslContext.transactional {
            customerRepository.deleteAll()
        }
    }.mapLeft { e: Throwable -> Error.Database(e) }

    suspend fun createCustomer(name: String) = catch {
        val customer = Customer(
            id = Customer.Id(UUID.randomUUID()), name = name
        )
        dslContext.transactional {
            customerRepository.save(customer)
        }
    }.mapLeft { e: Throwable ->
        when {
            e.isConstraintException("customers_name_key") -> Error.CreateCustomer.CustomerNameAlreadyExists(name)
            else -> Error.Database(e)
        }
    }

    suspend fun openAccount(customerId: Customer.Id, name: String): Either<Error.OpenAccount, Account> = catch {
        val account = Account(
            id = Account.Id(UUID.randomUUID()),
            customerId = customerId,
            name = name,
            balance = MonetaryAmount(0)
        )
        dslContext.transactional { accountRepository.save(account) }
    }.mapLeft { e: Throwable ->
        when {
            e.isConstraintException("accounts_customer_id_fkey") -> Error.OpenAccount.CustomerNotFound(customerId)
            else -> Error.Database(e)
        }
    }

    private fun Throwable.isConstraintException(constraintName: String) =
        this is DataAccessException && cause?.message?.contains(constraintName) == true

    suspend fun deposit(accountId: Account.Id, amount: MonetaryAmount): Either<Error.Deposit, Unit> = catch {
        dslContext.transactional {
            val deposit = Transaction.Deposit(
                id = Transaction.Id(UUID.randomUUID()), accountId = accountId, amount = amount
            )
            deposit.accountId.add(deposit.amount.value)
            transactionRepository.save(deposit)
        }
    }.handleError()

    suspend fun withdraw(accountId: Account.Id, amount: MonetaryAmount): Either<Error.Withdrawal, Unit> = catch {
        dslContext.transactional {
            val withdrawal = Transaction.Withdrawal(
                id = Transaction.Id(UUID.randomUUID()), accountId = accountId, amount = amount
            )
            withdrawal.accountId.add(-withdrawal.amount.value)
            transactionRepository.save(withdrawal)
        }
    }.handleError()

    suspend fun transfer(
        fromAccountId: Account.Id,
        toAccountId: Account.Id,
        amount: MonetaryAmount
    ): Either<Error.Transfer, Unit> = catch {
        dslContext.transactional {
            val transfer = Transaction.Transfer(
                id = Transaction.Id(UUID.randomUUID()),
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = amount
            )
            transfer.toAccountId.add(transfer.amount.value)
            transfer.fromAccountId.add(-transfer.amount.value)
            transactionRepository.save(transfer)
        }
    }.handleError()

    context (DSLContext) private suspend fun Account.Id.add(amount: Int) {
        accountRepository.add(this, amount).let { newBalance ->
            when {
                newBalance == null -> Error.Transaction.AccountNotFound(this).raise()
                newBalance < 0 -> Error.Transaction.CreditLineExceeded(newBalance).raise()
                else -> {}
            }
        }
    }

    suspend fun findAccountsWithTransactions(customerId: Customer.Id) =
        accountRepository.fetchAccountsWithTransactions(customerId)
}
