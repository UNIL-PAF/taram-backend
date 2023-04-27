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
        assert(summary.groups?.size == 4)

        summary.groups?.forEach{println(it.min)}

        val first = summary.groups?.get(0)
        println(first)

        assert(first?.expName == "14650")
        assert(roundNumber(first?.min) == roundNumber(0.5829123))
        assert(first?.max == 1743661.5)
        assert(roundNumber(first?.mean, 2) == roundNumber(10156.14, 2))
        assert(roundNumber(first?.median, 3) == roundNumber(1440.018, 3))
        assert(roundNumber(first?.median, 3) == roundNumber(1440.018, 3))
        assert(first?.sum == 5.83673466114926E7)
        assert(first?.nrValid == 5747)
        assert(roundNumber(first?.stdErr, 4) ==  roundNumber(671.3578, 4))
        assert(roundNumber(first?.coeffOfVar, 6) ==  roundNumber(5.011248, 6))
    }

}
