package ch.unil.pafanalysis.analysis.steps.group_filter

import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.common.Table
import org.springframework.stereotype.Service

@Service
class FixGroupFilterComputation() {

    fun run(table: Table?, params: GroupFilterParams?, columnInfo: ColumnInfo?): Table? {
        if (columnInfo?.columnMapping?.experimentDetails == null || columnInfo?.columnMapping?.experimentDetails.values.any { it.isSelected == true && it.group == null }) throw Exception(
            "Please specify your groups in the Analysis parameters."
        )
        val field = params?.field ?: columnInfo?.columnMapping?.intCol
        val expDetails = columnInfo?.columnMapping?.experimentDetails
        val validGroups = getValidGroups(table, expDetails, field, params?.zeroIsInvalid)

        val validRows: List<Boolean>? = when (params?.filterInGroup) {
            FilterInGroup.ONE_GROUP.value -> checkOneGroup(validGroups, params?.minNrValid)
            FilterInGroup.ALL_GROUPS.value -> checkAllGroups(validGroups, params?.minNrValid)
            FilterInGroup.TOTAL.value -> checkTotal(validGroups, params?.minNrValid)
            else -> throw Exception("FilterInGroup parameter must be defined.")
        }

        return remove(table, validRows)
    }

    private fun checkTotal(validGroups: List<List<Int>>?, minNrValid: Int?): List<Boolean>? {
        val validCounts =  validGroups?.fold(emptyList<Int>()) { acc, group ->
            if (acc.isEmpty()) group
            else acc.mapIndexed{ i, a -> a + group[i]}
        }
        return validCounts?.map{ it >= minNrValid!! }
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

    private fun getValidGroups(table: Table?, expDetails: Map<String, ExpInfo>?, field: String?, zeroIsInvalid: Boolean?): List<List<Int>>? {
        val headerGroups: Map<String?, List<Header>>? =
            table?.headers?.filter { it.experiment?.field == field}?.groupBy { expDetails?.get(it.experiment?.name)?.group }?.filter { it.key != null }

        return headerGroups?.mapValues { it ->
            it.value.fold(emptyList<Int>()) { acc, el ->
                val col = table?.cols?.get(el.idx)
                val l: List<Boolean> = col?.map { a ->
                    val r = a as? Double
                    (r?.isNaN() == false && (zeroIsInvalid != true || r != 0.0))
                }!!

                if (acc.isEmpty()) {
                    l.map{ if(it) 1 else 0 }
                } else {
                    acc.zip(l).map { if(it.second) it.first + 1 else it.first }
                }
            }
        }?.toList()?.map { it.second }
    }

    private fun remove(table: Table?, validRows: List<Boolean>?): Table? {
        val fltCols = table?.cols?.map { c -> c.filterIndexed { i, _ -> validRows!![i] } }
        return table?.copy(cols = fltCols)
    }

}