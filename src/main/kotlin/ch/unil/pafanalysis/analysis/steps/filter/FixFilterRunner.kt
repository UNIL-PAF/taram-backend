package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.common.ReadTableData
import org.springframework.stereotype.Service

@Service
class FixFilterRunner() {

    fun run(table: ReadTableData.Table?, params: FilterParams): ReadTableData.Table? {
        val table1 = removeContaminants(table, params)
        val table2 = removeReverse(table1, params)
        return removeOnlyIdentifiedBySite(table2, params)
    }

    private fun removeContaminants(table: ReadTableData.Table?, params: FilterParams): ReadTableData.Table? {
        return if (params.removePotentialContaminant == true) {
            remove(table, "Potential contaminant", "+")
        } else table
    }

    private fun removeOnlyIdentifiedBySite(table: ReadTableData.Table?, params: FilterParams): ReadTableData.Table? {
        return if (params.removeOnlyIdentifiedBySite == true) {
            remove(table, "Only identified by site", "+")
        } else table
    }

    private fun removeReverse(table: ReadTableData.Table?, params: FilterParams): ReadTableData.Table? {
        return if (params.removeReverse == true) {
            remove(table, "Reverse", "+")
        } else table
    }

    private fun remove(table: ReadTableData.Table?, name: String, match: String): ReadTableData.Table? {
        val header: ReadTableData.Header? = table?.headers?.find { it.name == name }
        if (header != null) {
            val fltRows = table?.rows?.filter { it[header.idx] != match }
            return table?.copy(rows = fltRows)
        }
        return table
    }


}