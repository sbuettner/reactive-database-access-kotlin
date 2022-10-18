package sbuetter.demo.model

import java.util.UUID

data class Customer(val id: Id, val name: String) {
    @JvmInline
    value class Id(val value: UUID)
}
