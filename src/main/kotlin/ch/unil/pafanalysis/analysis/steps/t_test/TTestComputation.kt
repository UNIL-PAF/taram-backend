package ch.unil.pafanalysis.analysis.steps.t_test

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import org.springframework.stereotype.Service

@Service
class TTestComputation {

    fun run(table: Table?, params: TTestParams?, step: AnalysisStep?): Triple<Table?, List<Header>?, TTest> {

        if (step?.columnInfo?.columnMapping?.experimentDetails == null || step?.columnInfo?.columnMapping?.experimentDetails.values.any { it.isSelected == true && it.group == null }) throw StepException(
            "Please specify your groups in the Analysis parameters."
        )

        if ((params?.firstGroup?.size ?: 0) == 0) {
            throw StepException("You must at least chose one valid pair of groups in first and second group.")
        }

        if(params?.firstGroup?.size != params?.secondGroup?.size){
            throw StepException("First and second groups don't have the same number of items. Please select pairs.")
        }

        val comparisions: List<GroupComp>? =
            params?.firstGroup?.zip(params?.secondGroup ?: emptyList())?.map { GroupComp(it.first, it.second) }

        val isLogVal = step?.commonResult?.intColIsLog
        val field = params?.field ?: step?.columnInfo?.columnMapping?.intCol

        val initialRes: Triple<Table?, List<Header>?, TTest> =
            Triple(table, table?.headers, TTest(comparisions = emptyList()))

        val res: Triple<Table?, List<Header>?, TTest> = comparisions?.fold(initialRes) { acc, comp ->
            computeComparision(comp, acc.first, field, acc.third, isLogVal, params, step?.columnInfo?.columnMapping?.experimentDetails)
        } ?: initialRes

        return res
    }

    private val readTableData = ReadTableData()

    private fun computeComparision(
        comp: GroupComp,
        table: Table?,
        field: String?,
        ttest: TTest,
        isLogVal: Boolean?,
        params: TTestParams?,
        expDetails: Map<String, ExpInfo>?
    ): Triple<Table?, List<Header>?, TTest> {
        val ints: Pair<List<List<Double>>, List<List<Double>>> =
            listOf(comp.group1, comp.group2).map { group ->
                // check if group is still valid
                val groupFound = table?.headers?.any { expDetails?.get(it.experiment?.name)?.group == group}
                if(groupFound != true) throw StepException("Condition [$group] doesn't exist.")
                readTableData.getDoubleMatrixByRow(table, field, expDetails, group).second
            }.zipWithNext().single()
        val rowInts: List<Pair<List<Double>, List<Double>>> = ints.first.zip(ints.second)
        val pVals = computeTTest(rowInts)
        val qVals: List<Double>? = if(params?.multiTestCorr != MulitTestCorr.NONE.value) multiTestCorr(pVals, params) else null
        val foldChanges = computeFoldChanges(rowInts, isLogVal)
        val signGroups = (qVals ?: pVals).map { it <= params?.signThres!! }
        val nrSign = signGroups.map { if (it) 1 else 0 }.sum()
        val (newTable, headers) = addResults(table, pVals, qVals, foldChanges, signGroups, comp)
        val newTtest = ttest.copy(
            comparisions = ttest.comparisions?.plusElement(
                TTestComparision(
                    comp.group1,
                    comp.group2,
                    nrSign
                )
            )
        )
        return Triple(newTable, headers, newTtest)
    }

    private fun addResults(
        table: Table?,
        pVals: List<Double>,
        qVals: List<Double>?,
        foldChanges: List<Double>,
        signGroups: List<Boolean>,
        comp: GroupComp
    ): Pair<Table?, List<Header>?> {
        val nrHeaders = table?.headers?.size!!
        val compName = "${comp.group1.trim()}-${comp.group2.trim()}"
        val pValHeader = listOf(Header(name = "p.value.$compName", idx = nrHeaders, ColType.NUMBER, Experiment(comp = comp)))
        val idxOffset = if(qVals == null) 0 else 1
        val foldHeader = listOf(
            Header(name = "log2.fold.change.$compName", idx = nrHeaders + 1 + idxOffset, ColType.NUMBER, Experiment(comp = comp)),
            Header(name = "is.significant.$compName", idx = nrHeaders + 2 + idxOffset, ColType.CHARACTER, Experiment(comp = comp))
        )
        val qValHeader = if(qVals == null) emptyList() else listOf(Header(name = "q.value.$compName", idx = nrHeaders + 1, ColType.NUMBER, Experiment(comp = comp)))
        val newHeaders: List<Header>? = table?.headers.plus(pValHeader).plus(qValHeader).plus(foldHeader)

        val pValCol = listOf<List<Any>>(pVals)
        val qValCol = if(qVals == null) emptyList<List<Any>>() else listOf(qVals)
        val foldCols = listOf<List<Any>>(foldChanges, signGroups.map { it.toString() })
        val addCols = pValCol.plus(qValCol).plus(foldCols)

        val newCols = table.cols?.plus(addCols)
        return Pair(Table(newHeaders, newCols), newHeaders)
    }

    private fun computeFoldChanges(ints: List<Pair<List<Double>, List<Double>>>, isLogVal: Boolean?): List<Double> {
        return ints.map { row ->
            val first = row.first.filter{!it.isNaN()}
            val second = row.second.filter{!it.isNaN()}
            if (isLogVal == true) {
                first.average() - second.average()
            } else {
                first.average() / second.average()
            }
        }
    }

    private fun multiTestCorr(pVals: List<Double>, params: TTestParams?): List<Double> {
        val code = RCode.create()
        code.addDoubleArray("p_vals", pVals.toDoubleArray())
        code.addRCode("corr_p_vals <- p.adjust(p_vals, method='${params?.multiTestCorr}')")
        val caller = RCaller.create(code, RCallerOptions.create())
        caller.runAndReturnResult("corr_p_vals")
        return caller.parser.getAsDoubleArray("corr_p_vals").toList()
    }

    private fun computeTTest(ints: List<Pair<List<Double>, List<Double>>>): List<Double> {
        val apacheTTest = org.apache.commons.math3.stat.inference.TTest()

        return ints.map { row ->
            val first = row.first.filter{!it.isNaN()}
            val second = row.second.filter{!it.isNaN()}

            val pVal = if(first.size < 2 || second.size < 2) Double.NaN else apacheTTest.homoscedasticTTest(first.toDoubleArray(), second.toDoubleArray())
            pVal
        }
    }
}