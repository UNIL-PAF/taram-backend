package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.common.ReadTableData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode


@SpringBootTest
class LogTransformationRunnerTests {

    @Autowired
    private val runner: LogTransformationRunner? = null

    private val readTableData = ReadTableData()
    private var ints: List<Pair<String, List<Double>>>? = null


    @BeforeEach
    fun init() {
        val filePath = "./src/test/resources/transformation/proteinGroups_1657194676917.txt"
        val selCols = listOf("Intensity KO-13703", "Intensity KO-13704", "Intensity KO-13705", "Intensity KO-13706",
            "Intensity KO-13707", "Intensity KO-13708", "Intensity KO-13709", "Intensity KO-13710", "Intensity WT-13695",
            "Intensity WT-13696", "Intensity WT-13697", "Intensity WT-13698", "Intensity WT-13699", "Intensity WT-13700",
            "Intensity WT-13701", "Intensity WT-13702")
        ints = readTableData.getColumnNumbers(filePath, selCols).mapIndexed{ i, a -> Pair(selCols[i], a)}
    }

    @Test
    fun log2Transformation() {
        val params = TransformationParams(transformationType = TransformationType.LOG2.value)
        val res = runner?.runTransformation(ints!!, params)
        val oneRes = BigDecimal(res!![0].second[0]).setScale(5, RoundingMode.HALF_EVEN)
        assert(oneRes.toDouble() == 23.98094)
    }

    @Test
    fun noneTransformation() {
        val params = TransformationParams(transformationType = TransformationType.NONE.value)
        val res = runner?.runTransformation(ints!!, params)
        val oneRes = res!![0].second[0]
        assert(oneRes == 16557000.0)
    }


}
