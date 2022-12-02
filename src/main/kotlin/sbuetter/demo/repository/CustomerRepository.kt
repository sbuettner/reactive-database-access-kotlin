package sbuetter.demo.repository

import kotlinx.coroutines.reactive.awaitSingle
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import sbuetter.demo.model.Customer
import sbuettner.demo.db.tables.records.CustomersRecord
import sbuettner.demo.db.tables.references.CUSTOMERS

@Component
class CustomerRepository() {

    context (DSLContext)
    suspend fun deleteAll() = deleteFrom(CUSTOMERS).awaitSingle()

    context (DSLContext)
    suspend fun save(customer: Customer) =
        insertInto(CUSTOMERS)
            .set(CUSTOMERS.ID, customer.id.value)
            .set(CUSTOMERS.NAME, customer.name)
            .returning()
            .awaitSingle().toCustomer()

    fun CustomersRecord.toCustomer() = Customer(id = Customer.Id(id!!), name = name!!)
}
