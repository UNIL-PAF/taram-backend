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
        table = readTableData.getTable(filePath, commonRes.headers)

        val mqMappingWithGroups = mqMapping.copy(experimentDetails = mqMapping.experimentDetails?.mapValues { (k, v) ->
            if (v.name?.contains("WT") == true) v.copy(group = "WT")
            else v.copy(group = "KO")
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
        val qValHeader = resTable?.headers?.find { it.name?.contains("adj.p.value")?:false }
        val qVals = resTable?.cols?.get(qValHeader?.idx!!)
            ?.map { if (qValHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        assert(
            roundNumbers(qVals?.subList(345, 350)) == roundNumbers(
                listOf<Double>(1.7703663447954E-5, 0.42081689973052, 0.764246735264101, 0.104041923799453, 0.566431416453898)
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

        assert(validIdx == listOf<Int>(34, 143, 216, 221, 297, 299, 300, 304, 340, 346, 369, 371, 396, 405, 407, 426, 490, 538, 724, 906, 1035, 1057, 1092, 1127, 1136, 1140, 1147, 1148, 1161, 1176, 1177, 1222, 1229, 1338, 1428, 1586, 1588, 1614, 1622, 1809, 1824, 1838, 1954, 1977, 2034, 2062, 2093, 2095, 2348, 2388, 2430, 2472, 2478, 2491, 2537, 2640, 2649, 2671, 2779, 2848, 2912, 2917, 2943, 3012, 3095, 3133, 3138, 3209, 3261, 3263, 3317, 3368, 3408, 3470, 3472, 3516, 3542, 3549, 3575, 3594, 3606, 3623, 3682, 3719, 3726, 3743, 3799, 3861, 3878, 3879, 3884, 3940, 4024, 4067, 4069, 4120, 4153, 4253, 4310, 4333, 4340, 4411, 4472, 4482, 4540, 4549, 4578, 4674, 4705, 4720, 4730, 4823, 4862, 4885, 4925, 4961, 4968, 4992, 5003))

        // verify t-test result
        assert(tTestRes.comparisions?.size == 1)
        assert(tTestRes.comparisions?.get(0)?.firstGroup == "KO")
        assert(tTestRes.comparisions?.get(0)?.secondGroup == "WT")
        assert(tTestRes.comparisions?.get(0)?.numberOfSignificant == 119)
    }

    private fun roundNumbers(list: List<Double>?): List<BigDecimal>? {
        return list?.map { roundNumber(it) }
    }

    private fun roundNumber(n: Double): BigDecimal {
        return BigDecimal(n).setScale(ROUNDING_PRECISION, RoundingMode.HALF_UP)
    }

}
