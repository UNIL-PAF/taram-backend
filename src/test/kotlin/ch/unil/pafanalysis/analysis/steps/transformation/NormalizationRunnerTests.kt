package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.analysis.steps.normalization.NormalizationCalculation
import ch.unil.pafanalysis.analysis.steps.normalization.NormalizationComputation
import ch.unil.pafanalysis.analysis.steps.normalization.NormalizationParams
import ch.unil.pafanalysis.analysis.steps.normalization.NormalizationType
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode


@SpringBootTest
class NormalizationRunnerTests {

    @Autowired
    private val runner: NormalizationComputation? = null

    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()

    private var ints: List<List<Double>>? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val commonRes = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).second
        val table = readTableData.getTable(filePath, commonRes.headers)
        ints = readTableData.getDoubleMatrix(table, "LFQ.intensity", null).second
    }

    @Test
    fun medianNormalization() {
        val params = NormalizationParams(normalizationType = NormalizationType.MEDIAN.value, normalizationCalculation = NormalizationCalculation.SUBSTRACTION.value)
        val res = runner?.runNormalization(ints!!, params)
        val oneRes = res!![0][0]
        assert(oneRes == -8296600.0)
    }

    @Test
    fun meanNormalization() {
        val params = NormalizationParams(normalizationType = NormalizationType.MEAN.value, normalizationCalculation = NormalizationCalculation.SUBSTRACTION.value)
        val res = runner?.runNormalization(ints!!, params)
        val oneRes = BigDecimal(res!![0][0])
        assert(oneRes == BigDecimal(-7.563853132827462E8))
    }

    @Test
    fun noneNormalization() {
        val params = NormalizationParams(normalizationType = NormalizationType.NONE.value, normalizationCalculation = NormalizationCalculation.SUBSTRACTION.value)
        val res = runner?.runNormalization(ints!!, params)
        val oneRes = res!![0][0]
        assert(oneRes == 5670400.0)
    }

}
