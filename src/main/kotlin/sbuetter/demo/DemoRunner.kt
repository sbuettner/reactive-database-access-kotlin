package sbuetter.demo

import arrow.core.continuations.either
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import sbuetter.demo.model.money

@Component
@Profile("!test")
class DemoRunner(val bank: Bank) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(DemoRunner::class.java)

    override fun run(vararg args: String?) {
        runBlocking {
            either {
                bank.clean().bind()

                val customer1 = bank.createCustomer("Customer 1").bind()
                val account1 = bank.openAccount(customer1.id, "Account 1").bind()

                val customer2 = bank.createCustomer("Customer 2").bind()
                val account2 = bank.openAccount(customer2.id, "Account 2").bind()

                bank.deposit(account1.id, 15.money())
                bank.withdraw(account1.id, 5.money()).bind()
                bank.deposit(account2.id, 25.money())
                bank.deposit(account2.id, 10.money())

                bank.transfer(account1.id, account2.id, 5.money())

                val accountsOfCustomer1 = bank.findAccountsWithTransactions(customer1.id).getOrNull()
                log.info("{}", accountsOfCustomer1)

                bank.withdraw(account1.id, 1000.money()).bind()
            }
        }
    }
}
