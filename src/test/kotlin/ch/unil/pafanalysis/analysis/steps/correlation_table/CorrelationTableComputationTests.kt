package ch.unil.pafanalysis.analysis.steps.correlation_table

import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.model.Header
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
class CorrelationTableComputationTests {

    @Autowired
    private val corrComp: CorrelationTableComputation? = null

    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()
    private var ints: List< List<Double>>? = null
    private var selHeaders: List<Header>? = null
    private var expDetails: Map<String, ExpInfo>? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val (colInfo, commonRes) = colParser!!.parse(filePath, resPath, ResultType.MaxQuant)

        val table = readTableData.getTable(filePath, commonRes.headers)
        val intMatrix = readTableData.getDoubleMatrix(table, "LFQ.intensity", null)

        val nrExps = 4

        expDetails = colInfo.experimentDetails
        ints = intMatrix.second.map{ it.map{ a -> if(a == 0.0) Double.NaN else ln(a) / ln(2.0) }}.take(nrExps)
        selHeaders = intMatrix.first.take(nrExps)
    }

    private val roundingPrecision = 7

    private fun roundNumber(n: Double?, precision: Int = roundingPrecision): BigDecimal {
        return if(n == null) BigDecimal(0)
        else BigDecimal(n).setScale(precision, RoundingMode.HALF_UP)
    }


    @Test
    fun runCorrelation() {
        val params = CorrelationTableParams(correlationType = CorrelationType.PEARSON.value)
        val corr = corrComp?.runCorrelation(ints, selHeaders, expDetails, params)

        assert(corr?.correlationMatrix?.size == selHeaders?.size)
        assert(corr?.correlationMatrix?.first()?.size == selHeaders?.size)
        assert(corr?.correlationMatrix?.first()?.first() == 1.0)
        assert(roundNumber(corr?.correlationMatrix?.first()?.get(1)) == roundNumber(0.926948787944895))
        assert(roundNumber(corr?.correlationMatrix?.get(1)?.get(3)) == roundNumber(0.946984778783149))
        assert(corr?.groupNames?.size == 4)
        println(corr?.experimentNames)
        assert(corr?.experimentNames == listOf("KO-13703", "KO-13704", "KO-13705", "KO-13706"))
    }

}
