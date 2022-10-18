package sbuetter.demo.model

@JvmInline
value class MonetaryAmount(val value: Int) {
    init {
        require(value >= 0) { "Value must be greater than or equal 0." }
    }
}

fun Int.money() = MonetaryAmount(this)
