package ch.unil.pafanalysis.analysis.steps.order_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncOrderColumnsRunner() : CommonStep() {

    @Autowired
    private var env: Environment? = null

    @Autowired
    private val analysisStepRepo: AnalysisStepRepository? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, OrderColumnsParams().javaClass)

            val origTable = ReadTableData().getTable(
                env?.getProperty("output.path").plus(newStep?.resultTablePath),
                newStep?.commonResult?.headers
            )

            val intCol = newStep?.columnInfo?.columnMapping?.intCol

            val newOrder = if(params.move !== null && params.newOrder == null){
                // we keep this for back-compatibility
                val order1 =  if(params.moveSelIntFirst == true) moveSelIntFirst(origTable, intCol) else origTable
                changeOrder(order1, params.move)
            } else getNewOrder(origTable, params.newOrder)

            WriteTableData().write(
                env?.getProperty("output.path")?.plus(newStep?.resultTablePath)!!,
                newOrder!!)

            val newImputationPath = moveImputed(newStep, newOrder.headers)

            newStep?.copy(
                imputationTablePath = newImputationPath,
                results = gson.toJson(OrderColumns()),
                commonResult = newStep?.commonResult?.copy(headers = newOrder?.headers)
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun moveImputed(step: AnalysisStep?, headers: List<Header>?): String? {
        return if(step?.imputationTablePath != null){
            val stepBefore = analysisStepRepo?.findById(step?.beforeId!!)

            // move imputated as well
            val imputed = ReadImputationTableData().getTable(
                env?.getProperty("output.path").plus(step?.imputationTablePath),
                stepBefore?.commonResult?.headers
            )

            val newHeaders = imputed.headers?.map{ impHead ->
                val selIdx = headers?.find{it.name == impHead.name}?.idx
                impHead.copy(idx = selIdx ?: throw StepException("Something messed up with the indexes.."))
            }

            val imputationFileName = step?.resultTablePath?.replace(".txt", "_imputation.txt")
            WriteImputationTableData().write(getOutputRoot() + imputationFileName, imputed.copy(headers = newHeaders))

            imputationFileName
        } else null
    }

    private fun changeOrder(table: Table?, move: List<MoveCol>?): Table? {
        return move?.fold(table){ acc, mov ->
            val newHeaders: List<Header>? = acc?.headers?.swap(mov.from!!, mov.to!!)
            val columns =  acc?.cols?.swap(mov.from!!, mov.to!!)
            Table(newHeaders?.mapIndexed{ i, h -> h.copy(idx = i)}, columns)
        }
    }

    private fun getNewOrder(table: Table?, newOrder: List<Int>?): Table? {
        // add new columns to the end if necessary
        val newOrder2 = if(table?.headers != null && newOrder != null && table.headers.size > newOrder.size){
            newOrder.plus(newOrder.size until table.headers.size)
        } else newOrder

        val newCols = newOrder2?.map{ a ->
            val selIdx = table?.headers?.withIndex()?.find{it.value.idx == a}?.index
                table?.cols?.get(selIdx!!)!!
        }
        val newHeaders = newOrder2?.mapIndexed{ i, a ->
            val selIdx = table?.headers?.withIndex()?.find{it.value.idx == a}?.index
            table?.headers?.get(selIdx!!)?.copy(idx = i)!!
        }
        return Table(headers = newHeaders, cols = newCols)
    }

    private fun <T> List<T>.swap(from: Int, to: Int): List<T>? {
        val my = this.toMutableList()
        val tmp = my[from]
        my.add(to, tmp)
        my.removeAt(from)
        return my.toList()
    }

    private fun moveSelIntFirst(table: Table?, intCol: String?): Table? {
        val emptyBeforeAndAfter = Pair(Pair(emptyList<Header>(), emptyList<Header>()), Pair(listOf(emptyList<Any>()), listOf(emptyList<Any>())))

        val newRes = table?.headers?.zip(table?.cols!!)?.fold(emptyBeforeAndAfter) { acc, col ->
            if(col.first.experiment != null && col.first.experiment?.field == intCol){
                val newHeaders: Pair<List<Header>, List<Header>>  = Pair(acc.first.first + col.first, acc.first.second)
                val addThis = if(acc.second.first.last().isEmpty()) listOf(col.second) else acc.second.first.plusElement(col.second)
                val newCols: Pair<List<List<Any>>, List<List<Any>>> = Pair(addThis, acc.second.second)
                Pair(newHeaders, newCols)
            }else{
                val newHeaders: Pair<List<Header>, List<Header>> = Pair(acc.first.first, acc.first.second + col.first)
                val addThis = if(acc.second.second.last().isEmpty()) listOf(col.second) else acc.second.second.plusElement(col.second)
                val newCols: Pair<List<List<Any>>, List<List<Any>>> = Pair(acc.second.first, addThis)
                Pair(newHeaders, newCols)
            }
        }

        val newHeaders = newRes?.first?.first?.plus(newRes?.first?.second)
        val corrIdxHeaders = newHeaders?.mapIndexed{ i, h -> h.copy(idx = i)}
        val newCols = newRes?.second?.first?.plus(newRes?.second?.second)

        return Table(corrIdxHeaders, newCols)
    }

}