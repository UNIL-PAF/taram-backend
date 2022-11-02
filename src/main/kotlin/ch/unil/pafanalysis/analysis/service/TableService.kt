package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.common.ImputationTable
import ch.unil.pafanalysis.common.Table
import org.springframework.stereotype.Service

@Service
class TableService {

    fun replaceImputedVals(table: Table, imputationTable: ImputationTable, replaceVal: Double): Table {
        return table.copy(cols = table.cols?.mapIndexed{ i, col ->
            col.mapIndexed{ k, v ->
                val idx: Int? = imputationTable.headers?.indexOfFirst{it.idx == i}
                if(idx != null && idx >= 0 && imputationTable.rows?.get(k)?.get(idx) == true){
                    replaceVal
                }else{
                    v
                }
            }
        })
    }

}