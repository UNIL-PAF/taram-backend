package ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.math.BigDecimal


@SpringBootTest
class ParseSpectronautSetupTests {

    @Autowired
    private val parser: ParseSpectronautSetup? = null

    /*
    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/spectronaut/"
        val filePath = resPath + "Gaspari-15321-32-Library_Report.setup.txt"
        val spectronautSetup = parser.parseSetup(filePath)
        val table = readTableData.getTable(filePath, commonRes.headers)
        ints = readTableData.getDoubleMatrix(table, "LFQ.intensity", null).second
    }
     */

    @Test
    fun gaspariSetup() {
        val resPath = "./src/test/resources/results/spectronaut/"
        val filePath = File(resPath + "Gaspari-15321-32-Library_Report.setup.txt")
        val spectronautSetup = parser?.parseSetup(filePath)

        println(spectronautSetup)

        assert(spectronautSetup?.analysisType == "blibla")
    }

}
