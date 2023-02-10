package ch.unil.pafanalysis.analysis.steps.imputation

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal


@SpringBootTest
class ImputationRunnerTests {

    @Autowired
    private val imputationComput: ImputationComputation? = null

    @Autowired
    val colParser: ColumnMappingParser? = null


    private val readTableData = ReadTableData()
    private var ints: List< List<Double>>? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val commonRes = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).second
        val table = readTableData.getTable(filePath, commonRes.headers)
        ints = readTableData.getDoubleMatrix(table, "LFQ.intensity").second
    }

    @Test
    fun normalImputationDefault() {
        val params = ImputationParams(imputationType = ImputationType.NORMAL.value, normImputationParams = NormImputationParams())
        val res = imputationComput?.runImputation(ints!!, params)
        val oneRes = BigDecimal(res!!.first[0][22])
        assert(ints!![0][22] == 0.0)
        assert(oneRes == BigDecimal(-1.431590438871017E10))
    }

    @Test
    fun normalImputationWithParams() {
        val params = ImputationParams(imputationType = ImputationType.NORMAL.value, normImputationParams = NormImputationParams(width = 0.5, downshift = 2.0, seed = 10))
        val res = imputationComput?.runImputation(ints!!, params)
        val oneRes = BigDecimal(res!!.first[0][22])
        assert(ints!![0][22] == 0.0)
        assert(oneRes == BigDecimal(-1.4063169578049812E10))
    }

    @Test
    fun normalImputationWithValue() {
        val params = ImputationParams(imputationType = ImputationType.VALUE.value, replaceValue = 0.5)
        val res = imputationComput?.runImputation(ints!!, params)
        val oneRes = BigDecimal(res!!.first[0][22])
        assert(ints!![0][22] == 0.0)
        assert(oneRes == BigDecimal(0.5))
    }

    @Test
    fun normalImputationWithNan() {
        val params = ImputationParams(imputationType = ImputationType.NAN.value)
        val res = imputationComput?.runImputation(ints!!, params)
        val oneRes = res!!.first[0][22]
        assert(ints!![0][22] == 0.0)
        assert(oneRes.isNaN())
    }

}
