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
import java.math.RoundingMode


@SpringBootTest
class TransformationRunnerTests {

    @Autowired
    private val imputation: ImputationRunner? = null

    @Autowired
    private val normalization: NormalizationRunner? = null

    @Autowired
    private val transformation: LogTransformationRunner? = null

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

    /*
    private fun writeTable(ints: List<Double>, fileName: String) {
        val writer = BufferedWriter(FileWriter(fileName))
        ints.forEach {
            writer.write(it.toString())
            writer.newLine()
        }
        writer.close()
    }
     */

    @Test
    fun defaultTransformationChain() {
        val params = TransformationParams(
            transformationType = TransformationType.LOG2.value,
            normalizationType = NormalizationType.MEDIAN.value,
            imputationType = ImputationType.NORMAL.value,
            imputationParams = ImputationParams()
        )
        val res1 = transformation?.runTransformation(ints!!, params)
        val res2 = normalization?.runNormalization(res1!!, params)
        val res3 = imputation?.runImputation(res2!!, params)
        val oneRes = BigDecimal(res3!![0].second[22]).setScale(5, RoundingMode.HALF_EVEN).toDouble()

        /*
        writeTable(ints!![0].second, "/tmp/orig.txt")
        writeTable(res1!![0].second, "/tmp/res1.txt")
        writeTable(res2!![0].second, "/tmp/res2.txt")
        writeTable(res3!![0].second, "/tmp/res3.txt")
         */

        assert(ints!![0].second[22] == 0.0)
        assert(oneRes == -4.55469)
    }

}
