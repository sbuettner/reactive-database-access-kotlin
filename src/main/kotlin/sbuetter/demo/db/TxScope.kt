package sbuetter.demo.db

import arrow.core.continuations.EffectScope
import org.jooq.DSLContext

class TxScope<R>(
    private val effectScope: EffectScope<R>,
    private val dsl: DSLContext
) : EffectScope<R> by effectScope, DSLContext by dsl
