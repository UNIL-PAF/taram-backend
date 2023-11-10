package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.common.ImputationTable
import ch.unil.pafanalysis.common.Table
import org.springframework.stereotype.Service

@Service
class TableService {

    /*
        give back the table with number of occurrences and number of lines with occurrences
     */
    fun replaceImputedVals(table: Table, imputationTable: ImputationTable, replaceVal: Double?): Triple<Table?, Int?, Int?> {
        val initialRes: Triple<List<List<Any>>, Int, List<Boolean>> = Triple(listOf<List<List<Any>>>(), 0, emptyList())
        val res: Triple<List<List<Any>>, Int, List<Boolean>>? = table.cols?.foldIndexed(initialRes){ i, acc, col ->
            val res2: Triple<List<Any>, Int, List<Boolean>> = col.foldIndexed(Triple(emptyList<Any>(), acc.second, listOf<Boolean>())){ k, acc2, row ->
                val idx: Int? = imputationTable.headers?.indexOfFirst{it.idx == table.headers?.get(i)?.idx}
                val doReplace = idx != null && idx >= 0 && imputationTable.rows?.get(k)?.get(idx) == true
                val v = if(doReplace) replaceVal!! else row
                Triple(acc2.first.plus(v), acc2.second + (if(doReplace) 1 else 0), acc2.third.plus(doReplace))
            }
            val rowsWithImp = if(acc.first.isNotEmpty()) acc.third.zip(res2.third).map{ a -> a.first || a.second } else res2.third
            Triple(acc.first.plusElement(res2.first), res2.second, rowsWithImp)
        }
        return Triple(table.copy(cols = res?.first), res?.second, res?.third?.count { it })
    }

}