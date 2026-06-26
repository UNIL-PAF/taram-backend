package ch.unil.pafanalysis.results.model

import ch.unil.pafanalysis.analysis.steps.StepException

enum class ResultType(val value: String) {
    MaxQuant("MaxQuant"),
    Spectronaut("Spectronaut"),
    FragPipe("FragPipe");

    companion object {
        fun fromValue(s: String?): ResultType {
            val resType = values().find { it.value == s }
            if (resType == null) { throw StepException("Invalid ResultType: $resType") }
            return resType
        }
    }

}