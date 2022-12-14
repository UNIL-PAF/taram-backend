package ch.unil.pafanalysis.analysis.steps

import ch.unil.pafanalysis.analysis.model.Header

data class CommonResult(
    //val intCol: String? = null,
    val numericalColumns: List<String?>? = null,
    val headers: List<Header>? = null,
    val intColIsLog: Boolean? = null
)