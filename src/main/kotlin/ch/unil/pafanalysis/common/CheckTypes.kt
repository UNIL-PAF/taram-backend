package ch.unil.pafanalysis.common

class CheckTypes {

    fun isNumerical(s: String): Boolean {
        return s.toDoubleOrNull() != null
    }
}