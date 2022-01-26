package ch.unil.pafanalysis.results.model

import java.time.LocalDateTime

class AvailableDir (
    val type: String? = null,
    val resFile: String? = null,
    val path: String? = null,
    val fileCreationDate: LocalDateTime? = null,
    val alreadyUsed: Boolean? = null
)