package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteTableData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.BufferedWriter
import java.io.FileWriter
import java.math.BigDecimal


@SpringBootTest
class ImputationRunnerTests {

    @Autowired
    private val runner: ImputationRunner? = null

    private val readTableData = ReadTableData()
    private var ints: List<Pair<String, List<Double>>>? = null


    @BeforeEach
    fun init() {
        val filePath = "./src/test/resources/transformation/proteinGroups_1657194676917.txt"
        val selCols = listOf(
            "LFQ intensity KO-13703",
            "LFQ intensity KO-13704",
            "LFQ intensity KO-13705",
            "LFQ intensity KO-13706",
            "LFQ intensity KO-13707",
            "LFQ intensity KO-13708",
            "LFQ intensity KO-13709",
            "LFQ intensity KO-13710",
            "LFQ intensity WT-13695",
            "LFQ intensity WT-13696",
            "LFQ intensity WT-13697",
            "LFQ intensity WT-13698",
            "LFQ intensity WT-13699",
            "LFQ intensity WT-13700",
            "LFQ intensity WT-13701",
            "LFQ intensity WT-13702"
        )
        ints = readTableData.getColumnNumbers(filePath, selCols).mapIndexed { i, a -> Pair(selCols[i], a) }
    }

    @Test
    fun normalImputationDefault() {
        val imputParams = ImputationParams()
        val params = TransformationParams(imputationType = ImputationType.NORMAL.value, imputationParams = imputParams)
        val res = runner?.runImputation(ints!!, params)
        val oneRes = BigDecimal(res!![0].second[22])
        assert(ints!![0].second[22] == 0.0)
        assert(oneRes == BigDecimal(-1.431590438871017E10))
    }

    @Test
    fun normalImputationWithParams() {
        val imputParams = ImputationParams(width = 0.5, downshift = 2.0, seed = 10)
        val params = TransformationParams(imputationType = ImputationType.NORMAL.value, imputationParams = imputParams)
        val res = runner?.runImputation(ints!!, params)
        val oneRes = BigDecimal(res!![0].second[22])
        assert(ints!![0].second[22] == 0.0)
        assert(oneRes == BigDecimal(-1.4063169578049812E10))
    }

    @Test
    fun normalImputationWithValue() {
        val imputParams = ImputationParams(replaceValue = 0.5)
        val params = TransformationParams(imputationType = ImputationType.VALUE.value, imputationParams = imputParams)
        val res = runner?.runImputation(ints!!, params)
        val oneRes = BigDecimal(res!![0].second[22])
        assert(ints!![0].second[22] == 0.0)
        assert(oneRes == BigDecimal(0.5))
    }

    @Test
    fun normalImputationWithNan() {
        val params = TransformationParams(imputationType = ImputationType.NAN.value)
        val res = runner?.runImputation(ints!!, params)
        val oneRes = res!![0].second[22]
        assert(ints!![0].second[22] == 0.0)
        assert(oneRes.isNaN())
    }
}
