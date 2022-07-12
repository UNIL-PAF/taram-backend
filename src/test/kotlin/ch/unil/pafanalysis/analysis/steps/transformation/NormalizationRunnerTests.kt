package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.common.ReadTableData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode


@SpringBootTest
class NormalizationRunnerTests {

    @Autowired
    private val runner: NormalizationRunner? = null

    private val readTableData = ReadTableData()
    private var ints: List<Pair<String, List<Double>>>? = null


    @BeforeEach
    fun init() {
        val filePath = "./src/test/resources/transformation/proteinGroups_1657194676917.txt"
        val selCols = listOf("LFQ intensity KO-13703", "LFQ intensity KO-13704", "LFQ intensity KO-13705", "LFQ intensity KO-13706",
            "LFQ intensity KO-13707", "LFQ intensity KO-13708", "LFQ intensity KO-13709", "LFQ intensity KO-13710", "LFQ intensity WT-13695",
            "LFQ intensity WT-13696", "LFQ intensity WT-13697", "LFQ intensity WT-13698", "LFQ intensity WT-13699", "LFQ intensity WT-13700",
            "LFQ intensity WT-13701", "LFQ intensity WT-13702")
        ints = readTableData.getColumnNumbers(filePath, selCols).mapIndexed{ i, a -> Pair(selCols[i], a)}
    }

    @Test
    fun medianNormalization() {
        val params = TransformationParams(normalizationType = NormalizationType.MEDIAN.value)
        val res = runner?.runNormalization(ints!!, params)
        val oneRes = res!![0].second[0]
        assert(oneRes == -8296600.0)
    }

    @Test
    fun meanNormalization() {
        val params = TransformationParams(normalizationType = NormalizationType.MEAN.value)
        val res = runner?.runNormalization(ints!!, params)
        val oneRes = BigDecimal(res!![0].second[0])
        assert(oneRes == BigDecimal(-7.563853132827462E8))
    }

    @Test
    fun noneNormalization() {
        val params = TransformationParams(normalizationType = NormalizationType.NONE.value)
        val res = runner?.runNormalization(ints!!, params)
        val oneRes = res!![0].second[0]
        assert(oneRes == 5670400.0)
    }

}
