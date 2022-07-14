package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.common.ReadTableData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode


@SpringBootTest
class ReadTableDataTests {
    private val readTableData = ReadTableData()

    @Test
    fun readTable() {
        val filePath = "./src/test/resources/common/proteinGroups.txt"
        val table = readTableData.getTable(filePath)

        assert(table!!.rows?.size == 5535)
        assert(table!!.headers!!.isNotEmpty())
        assert(table!!.headers!!.size == 219)
        assert(table!!.headers!![0].name == "Protein IDs")
        assert(table!!.headers!![0].idx == 0)
        assert(table!!.headers!![0].type == ReadTableData.ColType.CHARACTER)
    }

    @Test
    fun nanValuesAreNanValues() {
        val filePath = "./src/test/resources/common/proteinGroups.txt"
        val table = readTableData.getTable(filePath)

        val hFraction1 = table.headers!!.find{it.name == "Fraction 1"}
        assert(table.rows!![1][hFraction1!!.idx] == Double.NaN)
        assert(table.rows!![0][hFraction1!!.idx] == 24.0)
    }


}
