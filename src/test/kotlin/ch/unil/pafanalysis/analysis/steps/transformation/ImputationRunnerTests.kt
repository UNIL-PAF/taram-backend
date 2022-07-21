package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.common.WriteTableData
import ch.unil.pafanalysis.results.model.ResultType
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

    @Autowired
    val colParser: ColumnMappingParser? = null


    private val readTableData = ReadTableData()
    private var ints: List< List<Double>>? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val mqMapping = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).first
        val table = readTableData.getTable(filePath, mqMapping)
        ints = readTableData.getDoubleMatrix(table, "LFQ.intensity").second
    }

    @Test
    fun normalImputationDefault() {
        val imputParams = ImputationParams()
        val params = TransformationParams(imputationType = ImputationType.NORMAL.value, imputationParams = imputParams)
        val res = runner?.runImputation(ints!!, params)
        val oneRes = BigDecimal(res!![0][22])
        assert(ints!![0][22] == 0.0)
        assert(oneRes == BigDecimal(-1.431590438871017E10))
    }

    @Test
    fun normalImputationWithParams() {
        val imputParams = ImputationParams(width = 0.5, downshift = 2.0, seed = 10)
        val params = TransformationParams(imputationType = ImputationType.NORMAL.value, imputationParams = imputParams)
        val res = runner?.runImputation(ints!!, params)
        val oneRes = BigDecimal(res!![0][22])
        assert(ints!![0][22] == 0.0)
        assert(oneRes == BigDecimal(-1.4063169578049812E10))
    }

    @Test
    fun normalImputationWithValue() {
        val imputParams = ImputationParams(replaceValue = 0.5)
        val params = TransformationParams(imputationType = ImputationType.VALUE.value, imputationParams = imputParams)
        val res = runner?.runImputation(ints!!, params)
        val oneRes = BigDecimal(res!![0][22])
        assert(ints!![0][22] == 0.0)
        assert(oneRes == BigDecimal(0.5))
    }

    @Test
    fun normalImputationWithNan() {
        val params = TransformationParams(imputationType = ImputationType.NAN.value)
        val res = runner?.runImputation(ints!!, params)
        val oneRes = res!![0][22]
        assert(ints!![0][22] == 0.0)
        assert(oneRes.isNaN())
    }

}
