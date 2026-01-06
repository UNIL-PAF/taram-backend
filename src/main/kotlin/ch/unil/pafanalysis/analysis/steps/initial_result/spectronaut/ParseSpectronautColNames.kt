package ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut

object ParseSpectronautColNames {

    fun getCommonStartAndEnd(colNames: List<String>, selField: String? = null): Pair<String, String> {
        // remove the first part until the first_
        val cleanSelCols = colNames.map{it.replace(Regex("^.+?_(?=([A-Za-z]))"), "")}

        // in case nothing got removed, we have to remove up to the first number
        val cleanSelCols2 = if(cleanSelCols.first() == colNames.first()) {
            colNames.map { it.replace(Regex("^.+?_(?=([A-Za-z0-9]))"), "") }
        } else cleanSelCols

        val commonStart = cleanSelCols2.fold(cleanSelCols2.first()){ a, v ->
            v.commonPrefixWith(a)
        }.replace(Regex("^(\\d+)$"), "")
            .replace(Regex("_$"), "")
            .replace(Regex("_(\\d+)$"), "")

        val commonEnd = cleanSelCols2.fold(cleanSelCols2.first()){a, v -> v.commonSuffixWith(a)}
        val commonEnd2 = if(selField != null) commonEnd.replace(selField, "") else commonEnd

        return Pair(commonStart, commonEnd2)
    }

}