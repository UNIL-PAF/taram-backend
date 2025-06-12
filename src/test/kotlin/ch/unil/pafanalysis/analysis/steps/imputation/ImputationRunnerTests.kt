package ch.unil.pafanalysis.analysis.steps.imputation

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ln


@SpringBootTest
class ImputationRunnerTests {

    @Autowired
    private val imputationComput: ImputationComputation? = null

    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()
    private var intsBeforeLog: List< List<Double>>? = null
    private var ints: List< List<Double>>? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val commonRes = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).second
        val table = readTableData.getTable(filePath, commonRes.headers)
        intsBeforeLog = readTableData.getDoubleMatrix(table, "LFQ.intensity", null).second
        ints = intsBeforeLog?.map{ it.map{ a -> if(a == 0.0) Double.NaN else ln(a) / ln(2.0) }}
    }

    private val roundingPrecision = 7

    private fun roundNumber(n: Double?, precision: Int = roundingPrecision): BigDecimal {
        return if(n == null) BigDecimal(0)
        else BigDecimal(n).setScale(precision, RoundingMode.HALF_UP)
    }


    @Test
    fun normalImputationDefault() {
        val params = ImputationParams(imputationType = ImputationType.NORMAL.value, normImputationParams = NormImputationParams())
        val res = imputationComput?.runImputation(ints!!, params)
        val oneRes = res!!.first[0][22]
        assert(ints!![0][22].isNaN())
        assert(roundNumber(oneRes) == roundNumber(19.708867911011712))
    }

    @Test
    fun normalImputationWithParams() {
        val params = ImputationParams(imputationType = ImputationType.NORMAL.value, normImputationParams = NormImputationParams(width = 0.5, downshift = 2.0, seed = 10))
        val res = imputationComput?.runImputation(ints!!, params)
        val oneRes = res!!.first[0][22]
        assert(ints!![0][22].isNaN())
        assert(roundNumber(oneRes) == roundNumber(19.791049863700525))
    }

    @Test
    fun normalImputationWithValue() {
        val params = ImputationParams(imputationType = ImputationType.VALUE.value, replaceValue = 0.5)
        val res = imputationComput?.runImputation(ints!!, params)
        val oneRes = BigDecimal(res!!.first[0][22])
        assert(ints!![0][22].isNaN())
        assert(oneRes == BigDecimal(0.5))
    }

    @Test
    fun normalImputationWithNan() {
        val params = ImputationParams(imputationType = ImputationType.NAN.value)
        val res = imputationComput?.runImputation(intsBeforeLog!!, params)
        val oneRes = res!!.first[0][22]
        assert(intsBeforeLog!![0][22] == 0.0)
        assert(oneRes.isNaN())
    }


    @Test
    fun randomForestImputation() {
        val forestParams = ForestImputationParams(maxIter = 10, nTree = 100, fixedRes = true)
        val params = ImputationParams(imputationType = ImputationType.FOREST.value, forestImputationParams = forestParams)

        val subsetInts = ints?.map{it.take(100)}!!
        val res = imputationComput?.runImputation(subsetInts, params)

        val oneRes = res!!.first[0][22]
        assert(roundNumber(oneRes) == roundNumber(21.318434684724))

        val secondRes = res.first[13][80]
        assert(roundNumber(secondRes) == roundNumber(19.0151783985302))
    }

    @Test
    fun qrilcImputationWithParams() {
        val qrilcImputationParams = QrilcImputationParams(fixedRes = true)
        val params = ImputationParams(imputationType = ImputationType.QRILC.value, qrilcImputationParams = qrilcImputationParams)
        val subsetInts = ints?.map{it.take(100)}!!
        val res = imputationComput?.runImputation(subsetInts, params)
        val oneRes = res!!.first[0][22]
        assert(roundNumber(oneRes) == roundNumber(18.5511393511435))

        val secondRes = res.first[13][80]
        assert(roundNumber(secondRes) == roundNumber(17.8870635847231))

    }

}
