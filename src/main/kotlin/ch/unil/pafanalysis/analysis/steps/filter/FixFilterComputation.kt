package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import org.springframework.stereotype.Service

@Service
class FixFilterComputation() {

    fun run(table: Table?, params: FilterParams, resType: String?): Table? {
        val table1 = if(resType == ResultType.MaxQuant.value) {
            removeContaminantsMQ(table, params)
        }else{
            removeContaminantsSN(table, params)
        }
        val table2 = removeReverse(table1, params)
        return removeOnlyIdentifiedBySite(table2, params)
    }

    private fun removeContaminantsMQ(table: Table?, params: FilterParams): Table? {
        return if (params.removePotentialContaminant == true) {
            remove(table, "Potential.contaminant", "+")
        } else table
    }

    private fun removeContaminantsSN(table: Table?, params: FilterParams): Table? {
        return if (params.removePotentialContaminant == true) {
            remove(table, "PG.ProteinGroups", "^Cont_.+", true)
        } else table
    }

    private fun removeOnlyIdentifiedBySite(table: Table?, params: FilterParams): Table? {
        return if (params.removeOnlyIdentifiedBySite == true) {
            remove(table, "Only.identified.by.site", "+")
        } else table
    }

    private fun removeReverse(table: Table?, params: FilterParams): Table? {
        return if (params.removeReverse == true) {
            remove(table, "Reverse", "+")
        } else table
    }

    private fun remove(table: Table?, name: String, match: String, isRegex: Boolean = false): Table? {
        val header: Header = table?.headers?.find { it.name == name } ?: throw Exception("Cannot find column [$name].")
        if (header?.type != ColType.CHARACTER) throw Exception("Cannot filter on a numeric field [$name].")

        val keepRow = table?.cols?.get(header.idx)?.map {
            val s = it as? String ?: ""
            if(isRegex) !(Regex(match).containsMatchIn(s)) else s != match
        }

        val fltCols = table?.cols?.map { c -> c.filterIndexed { i, _ -> keepRow!![i] } }
        return table?.copy(cols = fltCols)
    }


}