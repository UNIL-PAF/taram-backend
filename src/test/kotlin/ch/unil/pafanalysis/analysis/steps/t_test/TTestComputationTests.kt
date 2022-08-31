package ch.unil.pafanalysis.analysis.steps.t_test

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
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

const val ROUNDING_PRECISION = 4

@SpringBootTest
class TTestComputationTests {

    @Autowired
    private val runner: TTestComputation? = null

    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()

    private var table: Table? = null
    private var colInfoWithoutGroups: ColumnInfo? = null
    private var colInfo: ColumnInfo? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = "./src/test/resources/results/maxquant/Grepper_preprocessed.txt"
        val mqMapping = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).first
        val tableWithoutGroups = readTableData.getTable(filePath, mqMapping)

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
    }

    /*
    @Test
    fun checkUndefinedGroupException() {
        val params = GroupFilterParams(8, FilterInGroup.ONE_GROUP.value, "Intensity")

        val exception: Exception = assertThrows { runner?.run(table, params, colInfoWithoutGroups) }
        val expectedMessage = "Please specify your groups in the 'Initial result' parameters."
        val actualMessage = exception.message

        println(actualMessage)

        assert(actualMessage!!.contains(expectedMessage))
    }

     */

    @Test
    fun compute2SidedTTest() {
        val params = TTestParams(".LFQ.intensity")
        val resTable = runner?.run(table, params, colInfo)
        val nrNewCols = 4

        // check if headers are added
        assert(table?.headers?.size?.plus(nrNewCols) == resTable?.first?.headers?.size)
        // check if columns are added
        assert(table?.cols?.size?.plus(nrNewCols) == resTable?.first?.cols?.size)
        // verify that nr of rows didnt change
        assert(table?.cols?.get(0)?.size == resTable?.first?.cols?.get(0)?.size)

        // verify p-values
        val pValHeader = resTable?.first?.headers?.find { it.name == "p.value" }
        val pVals = resTable?.first?.cols?.get(pValHeader?.idx!!)
            ?.map { if (pValHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        assert(
            roundNumbers(pVals?.take(5)) == roundNumbers(
                listOf<Double>(2.946683e-01, 2.555445e-01, 8.006673e-01, 1.831762e-01, 3.459196e-01)
            )
        )

        assert(roundNumber(pVals?.average()!!) == roundNumber(0.3466965))

        // verify q-values
        val qValHeader = resTable?.first?.headers?.find { it.name == "q.value" }
        val qVals = resTable?.first?.cols?.get(qValHeader?.idx!!)
            ?.map { if (qValHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        assert(
            roundNumbers(qVals?.subList(345, 350)) == roundNumbers(
                listOf<Double>(5.308474e-05, 9.999684e-01, 9.999684e-01, 9.999684e-01, 9.999684e-01)
            )
        )

        // verify fold changes
        val foldChangeHeader = resTable?.first?.headers?.find { it.name == "fold.change" }
        val foldChange = resTable?.first?.cols?.get(foldChangeHeader?.idx!!)
            ?.map { if (foldChangeHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        assert(
            roundNumbers(foldChange?.subList(345, 350)) == roundNumbers(
                listOf<Double>(-0.6753862, -0.0937025,  0.1106688, -0.5716700, -2.3772725)
            )
        )

    }

    private fun roundNumbers(list: List<Double>?): List<BigDecimal>? {
        return list?.map { roundNumber(it) }
    }

    private fun roundNumber(n: Double): BigDecimal {
        return BigDecimal(n).setScale(ROUNDING_PRECISION, RoundingMode.HALF_UP)
    }

}
