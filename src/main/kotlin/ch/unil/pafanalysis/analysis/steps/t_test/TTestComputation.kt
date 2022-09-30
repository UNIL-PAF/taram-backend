package ch.unil.pafanalysis.analysis.steps.t_test

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.model.ColumnMapping
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.Table
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import org.apache.commons.math3.stat.inference.TTest
import org.springframework.stereotype.Service

@Service
class TTestComputation {

    fun run(table: Table?, params: TTestParams?, columnInfo: ColumnInfo?): Triple<Table?, Int, List<Header>?> {

        if (columnInfo?.columnMapping?.experimentDetails == null || columnInfo?.columnMapping?.experimentDetails.values.any { it.isSelected == true && it.group == null }) throw StepException(
            "Please specify your groups in the 'Initial result' parameters."
        )

        if (columnInfo?.columnMapping?.experimentDetails.values.groupBy { it.group }.keys.size != 2) throw StepException(
            "You must have exactly 2 groups."
        )

        val groupVals = getGroupValues(table, params?.field ?: columnInfo?.columnMapping?.intCol)
        val pVals = computeTTest(groupVals, params)
        val qVals = multiTestCorr(pVals)
        val foldChanges = computeFoldChanges(groupVals, params)
        val signGroups = qVals.map { it <= params?.signThres!! }
        val nrSign = signGroups.map { if (it) 1 else 0 }.sum()

        val (newTable, headers) = addResults(table, pVals, qVals, foldChanges, signGroups)

        return Triple(newTable, nrSign, headers)
    }

    private fun addResults(
        table: Table?,
        pVals: List<Double>,
        qVals: List<Double>,
        foldChanges: List<Double>,
        signGroups: List<Boolean>
    ): Pair<Table?, List<Header>?> {
        val nrHeaders = table?.headers?.size!!
        val addHeaders: List<Header> = listOf(
            Header(name = "p.value", idx = nrHeaders, ColType.NUMBER),
            Header(name = "q.value", idx = nrHeaders + 1, ColType.NUMBER),
            Header(name = "fold.change", idx = nrHeaders + 2, ColType.NUMBER),
            Header(name = "is.significant", idx = nrHeaders + 3, ColType.CHARACTER),
        )
        val newHeaders: List<Header>? = table?.headers.plus(addHeaders)

        val addCols = listOf<List<Any>>(pVals, qVals, foldChanges, signGroups.map{it.toString()})
        val newCols = table.cols?.plus(addCols)
        return Pair(Table(newHeaders, newCols), newHeaders?.plus(addHeaders))
    }

    private fun computeFoldChanges(groupVals: List<List<List<Double>>?>, params: TTestParams?): List<Double> {
        return groupVals.map { row ->
            if (params?.valuesAreLog == true) {
                row!![0].average() - row!![1].average()
            } else {
                row!![0].average() / row!![1].average()
            }
        }
    }

    private fun multiTestCorr(pVals: List<Double>): List<Double> {
        val code = RCode.create()
        code.addDoubleArray("p_vals", pVals.toDoubleArray())
        code.addRCode("corr_p_vals <- p.adjust(p_vals, method='hochberg')")
        val caller = RCaller.create(code, RCallerOptions.create())
        caller.runAndReturnResult("corr_p_vals")
        return caller.parser.getAsDoubleArray("corr_p_vals").toList()
    }

    private fun computeTTest(groupVals: List<List<List<Double>>?>, params: TTestParams?): List<Double> {
        val apacheTTest = TTest()

        return groupVals.map { row ->
            val pVal = apacheTTest.homoscedasticTTest(row!![0].toDoubleArray(), row!![1].toDoubleArray())
            //val pVal = apacheTTest.tTest(row!![0].toDoubleArray(), row!![1].toDoubleArray())
            pVal
        }
    }

    private fun getGroupValues(table: Table?, field: String?): List<List<List<Double>>?> {
        val headerGroups: Map<String?, List<Header>>? =
            table?.headers?.filter { it.experiment?.field == field }?.groupBy { it.experiment?.group }

        val nrRows = table?.cols?.get(0)?.size

        return (0 until nrRows!!).map { i ->
            headerGroups?.mapValues { headerList ->

                headerList.value.map { header ->
                    table.cols[header.idx][i] as Double
                }
            }?.toList()?.map { it.second }
        }
    }
}