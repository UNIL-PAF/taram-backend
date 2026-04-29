package ch.unil.pafanalysis.analysis.steps.stat_test

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.limma.Limma
import ch.unil.pafanalysis.analysis.steps.limma.LimmaComputation
import ch.unil.pafanalysis.analysis.steps.limma.LimmaParams
import ch.unil.pafanalysis.analysis.steps.t_test.TTest
import ch.unil.pafanalysis.analysis.steps.t_test.TTestComputation
import ch.unil.pafanalysis.analysis.steps.t_test.TTestParams
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncStatTestRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    @Autowired
    val limmaComputation: LimmaComputation? = null

    @Autowired
    val tTestComputation: TTestComputation? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val res = computeStatTest(newStep)

            newStep?.copy(
                results = gson.toJson(res.statTest),
                commonResult = newStep.commonResult?.copy(headers = res.headers)
            )
        }
        tryToRun(funToRun, newStep)
    }

    private fun computeStatTest(step: AnalysisStep?): StatTestRes {
        val outputRoot = getOutputRoot()
        val statTestParams = gson.fromJson(step?.parameters, StatTestParams().javaClass)
        val table = readTableData.getTable(outputRoot + step?.resultTablePath, step?.commonResult?.headers)

        val (resTable, statTest) = if(statTestParams.statTestType == StatTestType.WELCH_T_TEST.value || statTestParams.statTestType == StatTestType.STUDENT_T_TEST.value) {
            computeTTest(table, statTestParams, step)
        } else computeLimma(table, statTestParams, step)

        writeTableData.write(outputRoot + step?.resultTablePath!!, resTable!!)
        return StatTestRes(statTest, resTable.headers)

    }

    data class StatTestRes(val statTest: StatTest?, val headers: List<Header>?)

    private fun computeTTest(table: Table, statTestParams: StatTestParams, step: AnalysisStep?): Pair<Table?, StatTest> {
        val tTestParams = TTestParams(
            field = statTestParams.field,
            firstGroup = statTestParams.firstGroup,
            secondGroup = statTestParams.secondGroup,
            multiTestCorr = statTestParams.multiTestCorr,
            signThres = statTestParams.signThres,
            valuesAreLog = statTestParams.valuesAreLog,
            paired = statTestParams.paired,
            filterOnValid = statTestParams.filterOnValid,
            minNrValid = statTestParams.minNrValid,
            equalVariance = if(statTestParams.statTestType == StatTestType.STUDENT_T_TEST.value) true else false,
        )
        val (resTable, _, tTestRes) = tTestComputation?.run(table, tTestParams, step)!!
        return Pair(resTable, getStatTest(tTestRes))
    }

    private fun getStatTest(tTest: TTest): StatTest {
        return StatTest(comparisions = tTest.comparisions?.map{ a ->
            StatTestComparision(
                firstGroup = a.firstGroup,
                secondGroup = a.secondGroup,
                numberOfSignificant = a.numberOfSignificant,
                nrPassedFilter = a.nrPassedFilter
            )
        })
    }

    private fun computeLimma(table: Table, statTestParams: StatTestParams, step: AnalysisStep?): Pair<Table?, StatTest> {
        val limmaParams = LimmaParams(
            field = statTestParams.field,
            firstGroup = statTestParams.firstGroup,
            secondGroup = statTestParams.secondGroup,
            multiTestCorr = statTestParams.multiTestCorr,
            signThres = statTestParams.signThres,
            valuesAreLog = statTestParams.valuesAreLog,
            paired = statTestParams.paired,
            filterOnValid = statTestParams.filterOnValid,
            minNrValid = statTestParams.minNrValid,
        )
        val (resTable, limmaRes) = limmaComputation?.run(table, limmaParams, step)!!
        return Pair(resTable, getStatTest(limmaRes))
    }

    private fun getStatTest(limma: Limma): StatTest {
        return StatTest(comparisions = limma.comparisions?.map{ a ->
            StatTestComparision(
                firstGroup = a.firstGroup,
                secondGroup = a.secondGroup,
                numberOfSignificant = a.numberOfSignificant,
                nrPassedFilter = a.nrPassedFilter
            )
        })
    }

}