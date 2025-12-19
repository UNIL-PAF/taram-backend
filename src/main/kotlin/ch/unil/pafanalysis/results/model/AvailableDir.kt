package ch.unil.pafanalysis.results.model

import java.time.LocalDateTime

data class AvailableDir (
    val type: String? = null,
    val resFile: String? = null,
    val path: String? = null,
    val fileCreationDate: LocalDateTime? = null,
    val alreadyUsed: Boolean? = false,
    val resFileList: List<String>? = null
)