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
import com.google.common.math.Quantiles
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncAddColumnRunner() : CommonStep() {

    @Autowired
    private var env: Environment? = null

    private val readTable = ReadTableData()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, AddColumnParams().javaClass)

            if (params?.newColName == null) throw StepException("The new column needs a name.")

            val origTable = ReadTableData().getTable(
                env?.getProperty("output.path").plus(newStep?.resultTablePath),
                newStep?.commonResult?.headers
            )

            val newTable = when (params.type) {
                SelColType.CHAR -> addNewCharCol(origTable, params)
                SelColType.NUM -> addNewNumCol(origTable, params)
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
        val selHeaders = origTable.headers?.filter { h -> params.selIdxs?.contains(h.idx) ?: false }
        if (selHeaders?.find { it.type == ColType.CHARACTER && params.type == SelColType.CHAR } == null) {
            throw StepException("You must select at least one column.")
        }

        val matrix: List<List<String>>? =
            readTable.getStringMatrixByRow(origTable, selHeaders)

        val escaped = RegexHelper().escapeSpecialChars(params.charColParams?.compVal!!, wildcardMatch = true)
        val regex = Regex(escaped)

        val matchRes = if (params.charColParams?.compOp == CompOp.MATCHES) "" else "+"
        val noMatchRes = if (params.charColParams?.compOp == CompOp.MATCHES) "+" else ""

        fun matchRow(row: List<String>): String {
            val isMatched = if (params.charColParams.compSel == CompSel.ALL) {
                row.all { a -> regex.matches(a) }
            } else {
                row.any { a -> regex.matches(a) }
            }
            return if (isMatched) matchRes else noMatchRes
        }

        val newCol: List<Any> = matrix?.map { row -> matchRow(row) } ?: emptyList()
        val newCols = origTable.cols?.plusElement(newCol)
        val newColIdx = origTable?.headers?.lastIndex?.plus(1)
        val newHeader = Header(name = params.newColName, idx = newColIdx, type = ColType.CHARACTER)
        val newHeaders = origTable.headers?.plusElement(newHeader)
        return Table(headers = newHeaders, cols = newCols)
    }

    private fun addNewNumCol(origTable: Table, params: AddColumnParams): Table {
        val selHeaders = origTable.headers?.filter { h -> params.selIdxs?.contains(h.idx) ?: false }
        if (selHeaders?.find { it.type == ColType.NUMBER && params.type == SelColType.NUM } == null) {
            throw StepException("You must select at least one column.")
        }

        val matrix = readTable.getDoubleMatrixByRow(origTable, selHeaders)

        val newCol: List<Any> = if(params?.numColParams?.mathOp == null){
            throw StepException("MathOp has to be defined.")
        }else{
            computeNumCol(matrix, params?.numColParams?.mathOp, params?.numColParams?.removeNaN)
        }

        val newCols = origTable.cols?.plusElement(newCol)
        val newColIdx = origTable?.headers?.lastIndex?.plus(1)
        val newHeader = Header(name = params.newColName, idx = newColIdx, type = ColType.NUMBER)
        val newHeaders = origTable.headers?.plusElement(newHeader)
        return Table(headers = newHeaders, cols = newCols)
    }

    private fun computeNumCol(matrix: List<List<Double>>, mathOp: MathOp, removeNaN: Boolean?): List<Double> {
        return matrix.map { row ->
            val myRow = if(removeNaN == true) row.filter{!it.isNaN()} else row
            when (mathOp) {
                MathOp.MAX -> myRow.maxOrNull()
                MathOp.MIN -> myRow.minOrNull()
                MathOp.SUM -> myRow.sum()
                MathOp.MEAN -> myRow.average()
                MathOp.MEDIAN -> Quantiles.median().compute(myRow)
            } ?: Double.NaN
        }
    }

}