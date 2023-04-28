package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode


@SpringBootTest
class SummaryStatTests {
    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()

    private val ROUNDING_PRECISION = 7

    private fun roundNumber(n: Double?, precision: Int = ROUNDING_PRECISION): BigDecimal {
        return if(n == null) BigDecimal(0)
        else BigDecimal(n).setScale(precision, RoundingMode.HALF_UP)
    }


    @Test
    fun checkFluderTable() {
        val filePath = "./src/test/resources/results/spectronaut/20220707_114227_Fluder-14650-53_Report_Copy.txt"
        val commonRes = colParser!!.parse(filePath, null, ResultType.Spectronaut).second
        val table = readTableData.getTable(filePath, commonRes.headers)
        val (selHeaders, ints) = readTableData.getDoubleMatrix(table, "Quantity")
        assert(ints.size == selHeaders.size)

        val summary = SummaryStatComputation().getSummaryStat(ints, selHeaders)
        assert(summary.expNames?.size == 4)

        assert(summary?.expNames?.get(0) == "14650")
        assert(roundNumber(summary?.min?.get(0)) == roundNumber(0.5829123))
        assert(summary?.max?.get(0) == 1743661.5)
        assert(roundNumber(summary?.mean?.get(0), 2) == roundNumber(10156.14, 2))
        assert(roundNumber(summary?.median?.get(0), 3) == roundNumber(1440.018, 3))
        assert(summary?.sum?.get(0) == 5.83673466114926E7)
        assert(summary?.nrValid?.get(0) == 5747)
        assert(roundNumber(summary?.stdErr?.get(0), 4) ==  roundNumber(671.3578, 4))
        assert(roundNumber(summary?.coefOfVar?.get(0), 6) ==  roundNumber(5.011248, 6))
    }


    @Test
    fun checkFluderTableBasicSummary() {
        val filePath = "./src/test/resources/results/spectronaut/20220707_114227_Fluder-14650-53_Report_Copy.txt"
        val commonRes = colParser!!.parse(filePath, null, ResultType.Spectronaut).second
        val table = readTableData.getTable(filePath, commonRes.headers)
        val (selHeaders, ints) = readTableData.getDoubleMatrix(table, "Quantity")
        assert(ints.size == selHeaders.size)

        val summary = SummaryStatComputation().getBasicSummaryStat(ints, selHeaders)

        assert(summary.mean?.size == 1)
        assert(roundNumber(summary?.mean?.get(0), 2) == roundNumber(10735.35, 2))
        assert(roundNumber(summary?.min?.get(0)) == roundNumber(0.5614336))
        assert(roundNumber(summary?.max?.get(0)) == roundNumber(5009068.0))
        assert(summary?.sum?.get(0) == 2.4552824429511464E8)
    }

}
