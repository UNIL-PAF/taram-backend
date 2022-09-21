package ch.unil.pafanalysis.analysis.steps.group_filter

import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.common.Table
import org.springframework.stereotype.Service

@Service
class FixGroupFilterRunner() {

    fun run(table: Table?, params: GroupFilterParams?, columnInfo: ColumnInfo?): Table? {
        if (columnInfo?.columnMapping?.experimentDetails == null || columnInfo?.columnMapping?.experimentDetails.values.any { it.isSelected == true && it.group == null }) throw Exception(
            "Please specify your groups in the Analysis parameters."
        )
        val validGroups = getValidGroups(table, params?.field)

        val validRows: List<Boolean>? = when (params?.filterInGroup) {
            FilterInGroup.ONE_GROUP.value -> checkOneGroup(validGroups, params?.minNrValid)
            FilterInGroup.ALL_GROUPS.value -> checkAllGroups(validGroups, params?.minNrValid)
            else -> throw Exception("FilterInGroup parameter must be defined.")
        }

        return remove(table, validRows)
    }

    private fun checkAllGroups(validGroups: List<List<Int>>?, minNrValid: Int?): List<Boolean>? {
        return validGroups?.fold(emptyList<Boolean>()) { acc, group ->
            val valids = group.map { it >= minNrValid!! }
            if (acc.isEmpty()) valids
            else {
                acc.mapIndexed { i, a ->
                    a && valids[i]
                }
            }
        }
    }

    private fun checkOneGroup(validGroups: List<List<Int>>?, minNrValid: Int?): List<Boolean>? {
        return validGroups?.fold(emptyList<Boolean>()) { acc, group ->
            val valids = group.map { it >= minNrValid!! }
            if (acc.isEmpty()) valids
            else {
                acc.mapIndexed { i, a ->
                    if (a) a
                    else valids[i]
                }
            }
        }
    }

    private fun getValidGroups(table: Table?, field: String?): List<List<Int>>? {
        val headerGroups: Map<String?, List<Header>>? =
            table?.headers?.filter { it.experiment?.field == field }?.groupBy { it.experiment?.group }

        return headerGroups?.mapValues { it ->
            it.value.fold(emptyList<Int>()) { acc, el ->
                val col = table?.cols?.get(el.idx)
                val l: List<Int> = col?.map { a ->
                    val r = a as? Double
                    if (r != Double.NaN && r != 0.0) 1 else 0
                }!!

                if (acc.isEmpty()) {
                    l
                } else {
                    acc.zip(l).map { it.first + it.second }
                }
            }
        }?.toList()?.map { it.second }
    }

    private fun remove(table: Table?, validRows: List<Boolean>?): Table? {
        val fltCols = table?.cols?.map { c -> c.filterIndexed { i, _ -> validRows!![i] } }
        return table?.copy(cols = fltCols)
    }

}