package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.Table
import org.springframework.stereotype.Service

@Service
class CustomFilterRunner() {

    fun run(table: Table?, params: FilterParams): Table? {
        if(params.colFilters?.isEmpty() == true) return table

        checkParams(table?.headers, params)

        println("here")
        println(params)
        return table
    }

    fun checkParams(headers: List<Header>?, params: FilterParams){
        params.colFilters?.forEach{ filter ->
            val col = headers?.find { filter.colName == it.name }
            col ?: throw StepException("Could not find column [${filter.colName}].")
            if(col.type == ColType.CHARACTER && filter.comparator.numOnly) throw StepException("You cannot use the comparator ${filter.comparator.symbol} on a column containing characters.")
            if(filter.comparator.numOnly && filter.compareToValue.toDoubleOrNull() == null) throw StepException("You cannot use ${filter.comparator.symbol} with a non-numeric value.")
        }
    }
}