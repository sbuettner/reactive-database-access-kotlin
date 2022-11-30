package sbuetter.demo.db

import kotlinx.coroutines.reactive.awaitSingle
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import sbuetter.demo.model.Customer
import sbuettner.demo.db.tables.records.CustomersRecord
import sbuettner.demo.db.tables.references.CUSTOMERS

@Component
class CustomerRepository(val dsl: DSLContext) {

    suspend fun deleteAll() = dsl.deleteFrom(CUSTOMERS).awaitSingle()

    suspend fun save(customer: Customer) = dsl.insertInto(CUSTOMERS)
        .set(CUSTOMERS.ID, customer.id.value)
        .set(CUSTOMERS.NAME, customer.name)
        .returning()
        .awaitSingle().toCustomer()

    fun CustomersRecord.toCustomer() = Customer(id = Customer.Id(id!!), name = name!!)
}
