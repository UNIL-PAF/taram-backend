package ch.unil.pafanalysis.analysis.steps.add_column

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.service.TableService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.RegexHelper
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncAddColumnRunner() : CommonStep() {

    @Autowired
    private var env: Environment? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, AddColumnParams().javaClass)

            if(params?.newColName == null) throw StepException("The new column needs a name.")

            val origTable = ReadTableData().getTable(
                env?.getProperty("output.path").plus(newStep?.resultTablePath),
                newStep?.commonResult?.headers
            )

            val newTable = when (params.type){
                SelColType.CHAR -> addNewCharCol(origTable, params)
                else -> throw StepException("Not implemented yet..")
            }

            WriteTableData().write(
                env?.getProperty("output.path")?.plus(newStep?.resultTablePath)!!,
                newTable!!
            )

            newStep?.copy(
                results = gson.toJson(
                    AddColumn()
                ),
                commonResult = newStep?.commonResult?.copy(headers = newTable?.headers)
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun addNewCharCol(origTable: Table, params: AddColumnParams): Table {
        val col: List<String>? = ReadTableData().getStringColumn(origTable, params.selectedColumn ?: throw StepException("Please choose a column."))

        val escaped = RegexHelper().escapeSpecialChars(params.charColParams?.strVal!!, wildcardMatch = true)
        val regex = Regex(escaped)

        val matchRes = if(params.charColParams?.charComp == CharComp.MATCHES_NOT) "" else "+"
        val noMatchRes = if(params.charColParams?.charComp == CharComp.MATCHES_NOT) "+" else ""

        val newCol: List<Any> = col?.map{ a -> if(regex.matches(a)) matchRes else noMatchRes } ?: emptyList()
        val newCols = origTable.cols?.plusElement(newCol)

        val newColIdx = origTable?.headers?.lastIndex?.plus(1) ?: throw StepException("Could not set new Idx.")
        val newHeader = Header(name = params.newColName, idx = newColIdx, type = ColType.CHARACTER)
        val newHeaders = origTable.headers?.plusElement(newHeader)

        return Table(headers = newHeaders, cols = newCols)
    }

}