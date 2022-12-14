package ch.unil.pafanalysis.analysis.steps.t_test

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode

@SpringBootTest
class GrepperTTestComputationTests {

    private val ROUNDING_PRECISION = 4

    @Autowired
    private val runner: TTestComputation? = null

    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()

    private var table: Table? = null
    private var colInfoWithoutGroups: ColumnInfo? = null
    private var colInfo: ColumnInfo? = null
    private var step: AnalysisStep? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = "./src/test/resources/results/maxquant/Grepper_preprocessed.txt"
        val (mqMapping, commonRes) = colParser!!.parse(filePath, resPath, ResultType.MaxQuant)
        val tableWithoutGroups = readTableData.getTable(filePath, commonRes.headers)

        val mqMappingWithGroups = mqMapping.copy(experimentDetails = mqMapping.experimentDetails?.mapValues { (k, v) ->
            if (v.name?.contains("WT") == true) v.copy(group = "WT")
            else v.copy(group = "KO")
        })

        table = tableWithoutGroups.copy(headers = tableWithoutGroups.headers?.map {
            it.copy(
                experiment = it.experiment?.copy(group = if (it.experiment?.name?.contains("WT") == true) "WT" else "KO")
            )
        })

        colInfoWithoutGroups = ColumnInfo(columnMapping = mqMapping, columnMappingHash = null)
        colInfo = ColumnInfo(columnMapping = mqMappingWithGroups, columnMappingHash = null)

        step = AnalysisStep(columnInfo = colInfo, commonResult = CommonResult(intColIsLog = true))
    }

    @Test
    fun compute2SidedTTest() {
        val params = TTestParams("LFQ.intensity", firstGroup = listOf("KO"), secondGroup = listOf("WT"))
        val (resTable, headers, tTestRes) = runner?.run(table, params, step)!!

        // check if headers are added
        val nrNewCols = 4
        assert(table?.headers?.size?.plus(nrNewCols) == resTable?.headers?.size)
        // check if columns are added
        assert(table?.cols?.size?.plus(nrNewCols) == resTable?.cols?.size)
        // verify that nr of rows didnt change
        assert(table?.cols?.get(0)?.size == resTable?.cols?.get(0)?.size)

        // verify p-values
        val pValHeader = resTable?.headers?.find { it.name?.contains("p.value")?:false }
        val pVals = resTable?.cols?.get(pValHeader?.idx!!)
            ?.map { if (pValHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        assert(
            roundNumbers(pVals?.take(5)) == roundNumbers(
                listOf<Double>(2.946683e-01, 2.555445e-01, 8.006673e-01, 1.831762e-01, 3.459196e-01)
            )
        )

        assert(roundNumber(pVals?.average()!!) == roundNumber(0.3466965))

        // verify q-values
        val qValHeader = resTable?.headers?.find { it.name?.contains("q.value")?:false }
        val qVals = resTable?.cols?.get(qValHeader?.idx!!)
            ?.map { if (qValHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        assert(
            roundNumbers(qVals?.subList(345, 350)) == roundNumbers(
                listOf<Double>(5.308474e-05, 9.999684e-01, 9.999684e-01, 9.999684e-01, 9.999684e-01)
            )
        )

        // verify fold changes
        val foldChangeHeader = resTable?.headers?.find { it.name?.contains("fold.change")?:false }
        val foldChange = resTable?.cols?.get(foldChangeHeader?.idx!!)
            ?.map { if (foldChangeHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        assert(
            roundNumbers(foldChange?.subList(345, 350)) == roundNumbers(
                listOf<Double>(-0.6753862, -0.0937025,  0.1106688, -0.5716700, -2.3772725)
            )
        )

        // verify significant indexes
        val isSignHeader = resTable?.headers?.find { it.name?.contains("is.significant")?:false }
        val isSign = resTable?.cols?.get(isSignHeader?.idx!!)
            ?.map { if (isSignHeader.type == ColType.CHARACTER) it as? String ?: "" else "" }
        val validIdx = isSign?.foldIndexed(emptyList<Int>()){i, acc, v -> if(v == "true") acc.plus(i+1) else acc }
        assert(validIdx == listOf<Int>(299, 346, 1057, 1977, 2430, 3138, 4153, 4411, 4885))

        // verify t-test result
        assert(tTestRes.comparisions?.size == 1)
        assert(tTestRes.comparisions?.get(0)?.firstGroup == "KO")
        assert(tTestRes.comparisions?.get(0)?.secondGroup == "WT")
        assert(tTestRes.comparisions?.get(0)?.numberOfSignificant == 9)
    }

    private fun roundNumbers(list: List<Double>?): List<BigDecimal>? {
        return list?.map { roundNumber(it) }
    }

    private fun roundNumber(n: Double): BigDecimal {
        return BigDecimal(n).setScale(ROUNDING_PRECISION, RoundingMode.HALF_UP)
    }

}
