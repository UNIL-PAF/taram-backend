package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CustomFilterComputation() {

    private val readTableData = ReadTableData()

    fun run(table: Table?, params: FilterParams): Table? {
        if(params.colFilters?.isNotEmpty() != true) return table

        // check if the parameters are ok
        checkParams(table?.headers, params)

        val keepRows: List<Boolean>? = params.colFilters?.fold(emptyList()){ a, v ->
            val keep =  applyFilter(table, v)!!
            if(a.isNotEmpty()){
                a.zip(keep).map{ it.first && it.second }
            }else{
                keep
            }
        }

        val fltCols = table?.cols?.map { c -> c.filterIndexed { i, _ -> keepRows!![i] } }
        return table?.copy(cols = fltCols)
    }

    fun checkParams(headers: List<Header>?, params: FilterParams){
        params.colFilters?.forEach{ filter ->
            val col = headers?.find { filter.colName == it.name }
            col ?: throw StepException("Could not find column [${filter.colName}].")
            if(col.type == ColType.CHARACTER && filter.comparator.numOnly) throw StepException("You cannot use the comparator ${filter.comparator.symbol} on a column containing characters.")
            if(filter.comparator.numOnly && filter.compareToValue.toDoubleOrNull() == null) throw StepException("You cannot use ${filter.comparator.symbol} with a non-numeric value.")
        }
    }

    fun applyFilter(table: Table?, colFilter: ColFilter): List<Boolean>? {
        val col = table?.headers?.find { colFilter.colName == it.name }
        return if(colFilter.comparator.numOnly || col?.type == ColType.NUMBER)
            applyNumFilter(table, col?.name, colFilter)
        else
            applyCharFilter(table, col?.name, colFilter)
    }

    fun applyNumFilter(table: Table?, columnName: String?, colFilter: ColFilter): List<Boolean>? {
        val colVals = readTableData.getDoubleColumn(table, columnName!!)
        return colVals?.map{
            val keep = compareNum(it, colFilter?.compareToValue.toDouble(), colFilter.comparator)
            if(colFilter.removeSelected) !keep else keep
        }
    }

    fun compareNum(v1: Double, v2: Double, comparator: Comparator): Boolean {
        return when(comparator){
            Comparator.GE -> v1 >= v2
            Comparator.EQ -> v1 == v2
            Comparator.GT -> v1 > v2
            Comparator.NOT -> v1 != v2
            Comparator.SE -> v1 <= v2
            Comparator.ST -> v1 < v2
        }
    }

    fun applyCharFilter(table: Table?, columnName: String?, colFilter: ColFilter): List<Boolean>? {
        val colVals = readTableData.getStringColumn(table, columnName!!)
        return colVals?.map{
            val keep = compareString(it, colFilter?.compareToValue, colFilter.comparator)
            if(colFilter.removeSelected) !keep else keep
        }
    }

    fun compareString(v1: String, v2: String, comparator: Comparator): Boolean {
        // replace wildcard to regex .*
        val regex: Regex = Regex(v2.replace("*", ".*").replace("+", "\\+"))

        return when(comparator){
            Comparator.EQ -> regex.matches(v1)
            Comparator.NOT -> ! regex.matches(v1)
            else -> throw StepException("Invalid comparator ${comparator.symbol}.")
        }
    }
}
