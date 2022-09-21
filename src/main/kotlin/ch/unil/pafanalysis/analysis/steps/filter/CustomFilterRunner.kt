package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.common.Table
import org.springframework.stereotype.Service

@Service
class CustomFilterRunner() {

    fun run(table: Table?, params: FilterParams): Table? {
        println("here")
        println(params)
        return table
    }
}