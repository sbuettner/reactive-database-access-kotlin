package sbuetter.demo.model

sealed interface Error {

    data class Database(val e: Throwable) : CreateCustomer, Deposit, Transaction, OpenAccount

    sealed interface CreateCustomer : Error {
        data class CustomerNameAlreadyExists(val name: String) : CreateCustomer
    }

    sealed interface OpenAccount : Error {
        data class CustomerNotFound(val customerId: Customer.Id) : OpenAccount
    }

    sealed interface Deposit : Error

    sealed interface Withdrawal : Error

    sealed interface Transfer : Error

    sealed interface Transaction : Withdrawal, Transfer, Deposit {
        data class AccountNotFound(val accountId: Account.Id) : Transaction

        data class CreditLineExceeded(val balanceValue: Int) : Transaction
    }
}
