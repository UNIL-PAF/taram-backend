package ch.unil.pafanalysis.results.model

enum class ResultType(val value: String) {
    MaxQuant("MaxQuant"),
    Spectronaut("Spectronaut"),
    FragPipe("FragPipe");

    companion object {
        fun fromValue(s: String?): ResultType? {
            return values().find { it.value == s }
        }
    }

}