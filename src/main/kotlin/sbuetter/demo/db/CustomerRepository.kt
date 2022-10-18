package sbuetter.demo.db

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitLast
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import sbuetter.demo.model.Customer
import sbuettner.demo.db.tables.Customers.CUSTOMERS
import sbuettner.demo.db.tables.records.CustomersRecord

@Component
class CustomerRepository(private val dsl: DSLContext) {

    suspend fun deleteAll() = dsl.deleteFrom(CUSTOMERS).awaitLast()

    suspend fun save(customer: Customer) = dsl.insertInto(CUSTOMERS)
        .set(CUSTOMERS.ID, customer.id.value)
        .set(CUSTOMERS.NAME, customer.name)
        .returning()
        .awaitFirst().toCustomer()

    fun CustomersRecord.toCustomer() = Customer(id = Customer.Id(id), name = name)
}
