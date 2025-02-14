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
class MaryamTTestComputationTests {

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
        val resPath = "./src/test/resources/results/maxquant/Maryam/14448-50-517-19-602-04/"
        val filePath = "./src/test/resources/results/maxquant/Maryam/14448-50-517-19-602-04/Maryam_preprocessed.txt"
        val (mqMapping, commonRes) = colParser!!.parse(filePath, resPath, ResultType.MaxQuant)
        table = readTableData.getTable(filePath, commonRes.headers)

        val grp30 = listOf("14517", "14448", "14602")
        val grp30_e2 = listOf("14518", "14449", "14603")
        //val nes = listOf("14519", "14450", "14604")

        val mqMappingWithGroups = mqMapping.copy(experimentDetails = mqMapping.experimentDetails?.mapValues { (k, v) ->
            if (grp30.contains(v.name)) v.copy(group = "GRP30")
            else if (grp30_e2.contains(v.name)) v.copy(group = "GRP30-E2")
            else v.copy(group = "NES")
        })

        colInfoWithoutGroups = ColumnInfo(columnMapping = mqMapping, columnMappingHash = null)
        colInfo = ColumnInfo(columnMapping = mqMappingWithGroups, columnMappingHash = null)

        step = AnalysisStep(columnInfo = colInfo, commonResult = CommonResult(intColIsLog = true))
    }

    @Test
    fun compute2SidedTTest() {
        val params = TTestParams(
            "LFQ.intensity",
            firstGroup = listOf("GRP30", "GRP30", "GRP30-E2"),
            secondGroup = listOf("GRP30-E2", "NES", "NES")
        )
        val (resTable, headers, tTestRes) = runner?.run(table, params, step)!!

        // check if headers are added
        val nrNewCols = 15
        assert(table?.headers?.size?.plus(nrNewCols) == resTable?.headers?.size)
        // check if columns are added
        assert(table?.cols?.size?.plus(nrNewCols) == resTable?.cols?.size)
        // verify that nr of rows didnt change
        assert(table?.cols?.get(0)?.size == resTable?.cols?.get(0)?.size)


        // verify p-values
        val pValHeader = resTable?.headers?.find { it.name?.contains("p.value") ?: false }
        val pVals = resTable?.cols?.get(pValHeader?.idx!!)
            ?.map { if (pValHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        assert(
            roundNumbers(pVals?.take(5)) == roundNumbers(
                listOf<Double>(
                    0.5673984226054323,
                    0.32663381017744275,
                    0.586489686993912,
                    0.044959724126917976,
                    0.87096725404252
                )
            )
        )

        assert(roundNumber(pVals?.average()!!) == roundNumber(0.549109938915508))


        // verify q-values
        val qValHeader = resTable?.headers?.find { it.name?.contains("adj.p.value") ?: false }
        val qVals = resTable?.cols?.get(qValHeader?.idx!!)
            ?.map { if (qValHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        val signQPos = qVals?.mapIndexed { i, q -> if (q <= 0.05) i else null }?.filterNotNull()

        assert(
            signQPos == listOf(4385)
        )


// verify fold changes
        val foldChangeHeader = resTable?.headers?.find { it.name?.contains("fold.change") ?: false }
        val foldChange = resTable?.cols?.get(foldChangeHeader?.idx!!)
            ?.map { if (foldChangeHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        assert(
            roundNumbers(foldChange?.subList(345, 350)) == roundNumbers(
                listOf<Double>(
                    0.10614377924282259,
                    -0.5539976623354583,
                    0.09686638273572123,
                    0.19979147484744697,
                    -0.44947014924931983
                )
            )
        )

// verify significant indexes
        val isSignHeader = resTable?.headers?.find { it.name?.contains("is.significant") ?: false }
        val isSign = resTable?.cols?.get(isSignHeader?.idx!!)
            ?.map { if (isSignHeader.type == ColType.CHARACTER) it as? String ?: "" else "" }
        val validIdx = isSign?.foldIndexed(emptyList<Int>()) { i, acc, v -> if (v == "true") acc.plus(i + 1) else acc }

        assert(validIdx == listOf<Int>(4386))


// verify t-test result
        assert(tTestRes.comparisions?.size == 3)
        assert(tTestRes.comparisions?.get(0)?.firstGroup == "GRP30")
        assert(tTestRes.comparisions?.get(0)?.secondGroup == "GRP30-E2")
        assert(tTestRes.comparisions?.get(0)?.numberOfSignificant == 1)

    }

    private fun roundNumbers(list: List<Double>?): List<BigDecimal>? {
        return list?.map { roundNumber(it) }
    }

    private fun roundNumber(n: Double): BigDecimal {
        return BigDecimal(n).setScale(ROUNDING_PRECISION, RoundingMode.HALF_UP)
    }


}
