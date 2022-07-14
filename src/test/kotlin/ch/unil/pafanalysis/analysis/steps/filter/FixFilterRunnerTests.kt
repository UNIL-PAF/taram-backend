package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.common.ReadTableData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode


@SpringBootTest
class FixFilterRunnerTests {

    @Autowired
    private val runner: FixFilterRunner? = null

    private val readTableData = ReadTableData()

    private var table: ReadTableData.Table? = null

    @BeforeEach
    fun init() {
        val filePath = "./src/test/resources/filter/proteinGroups.txt"
        var table = readTableData.getTable(filePath)
    }

    @Test
    fun dummy() {
        assert(table!!.headers.size == 234)
    }


}
