package ch.unil.pafanalysis.common

class RegexHelper {

    fun escapeSpecialChars(s: String, wildcardMatch: Boolean? = false): String {
        // \.[]{}()<>*+-=!?^$|
        val escapeChars =
            listOf<Char>('\\', '.', '[', ']', '{', '}', '(', ')', '<', '>', '+', '-', '=', '!', '?', '^', '$', '|')

        return s.map{ c: Char ->
            if (escapeChars.find{it == c} != null) "\\" + c
            else if (c == '*') { if(wildcardMatch == true) ".+" else "\\*" }
            else c

        }.joinToString("")
    }
}