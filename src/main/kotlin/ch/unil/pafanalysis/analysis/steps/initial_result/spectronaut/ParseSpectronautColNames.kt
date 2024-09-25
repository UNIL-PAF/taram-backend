package ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut

object ParseSpectronautColNames {

    fun getCommonStartAndEnd(colNames: List<String>, selField: String? = null): Pair<String, String> {
        // remove the first part until the first_
        val cleanSelCols = colNames.map{it.replace(Regex("^.+?_(?=([A-Z|a-z]))"), "")}

        val commonStart = cleanSelCols.fold(cleanSelCols.first()){ a, v -> v.commonPrefixWith(a)}
            .replace(Regex("_(\\d+)$"), "")
            .replace(Regex("_$"), "")

        val commonEnd = cleanSelCols.fold(cleanSelCols.first()){a, v -> v.commonSuffixWith(a)}
        val commonEnd2 = if(selField != null) commonEnd.replace(selField, "") else commonEnd

        return Pair(commonStart, commonEnd2)
    }

}