package ch.unil.pafanalysis.analysis.steps

import ch.unil.pafanalysis.analysis.model.Header

data class CommonResult(
    val headers: List<Header>? = null,
    val intColIsLog: Boolean? = null
)