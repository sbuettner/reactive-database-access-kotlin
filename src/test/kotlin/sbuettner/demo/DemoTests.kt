package sbuettner.demo

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import sbuetter.demo.Bank
import sbuetter.demo.DemoApplication
import sbuetter.demo.model.Account
import sbuetter.demo.model.Customer
import sbuetter.demo.model.Error
import sbuetter.demo.model.money
import java.util.UUID

@SpringBootTest(classes = [DemoApplication::class])
@ActiveProfiles("test")
class DemoTests(@Autowired val bank: Bank) {

    @BeforeEach
    fun clean() {
        runBlocking {
            bank.clean().getOrNull()
        }
    }

    @Test
    fun testBank() {
        runBlocking {
            val customer = bank.createCustomer("Customer").getOrNull()!!
            val account = bank.openAccount(customer.id, "Account").getOrNull()!!
            customer.id.shouldBe(account.customerId)
            account.balance.value.shouldBe(0)

            val randomCustomerId = Customer.Id(UUID.randomUUID())
            bank.openAccount(randomCustomerId, "Account")
                .shouldBeLeft(Error.OpenAccount.CustomerNotFound(randomCustomerId))

            bank.deposit(account.id, 10.money())
            val accounts = bank.findAccountsWithTransactions(customer.id).getOrNull()!!
            accounts.first().account.balance.value.shouldBe(10)

            bank.withdraw(account.id, 1000.money())
                .shouldBeLeft(Error.Transaction.CreditLineExceeded(-990))

            bank.withdraw(account.id, 10.money()).shouldBeRight()
            bank.deposit(account.id, 10.money())

            val expectedError = Error.CreateCustomer.CustomerNameAlreadyExists(customer.name)
            bank.createCustomer(customer.name).shouldBeLeft(expectedError)

            val otherCustomer = bank.createCustomer("Other customer").getOrNull()!!
            val otherAccount = bank.openAccount(otherCustomer.id, "Other Account").getOrNull()!!

            bank.transfer(account.id, otherAccount.id, 5.money()).shouldBeRight()

            bank.transfer(account.id, otherAccount.id, 1000.money())
                .shouldBeLeft(Error.Transaction.CreditLineExceeded(-995))

            val randomAccountId = Account.Id(UUID.randomUUID())
            bank.transfer(account.id, randomAccountId, 5.money())
                .shouldBeLeft(Error.Transaction.AccountNotFound(randomAccountId))

            bank.deposit(randomAccountId, 5.money())
                .shouldBeLeft(Error.Transaction.AccountNotFound(randomAccountId))
        }
    }
}
