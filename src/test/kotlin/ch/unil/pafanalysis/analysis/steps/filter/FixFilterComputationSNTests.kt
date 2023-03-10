package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest


@SpringBootTest
class FixFilterComputationSNTests {

    @Autowired
    private val runner: FixFilterComputation? = null

    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()

    private var table: Table? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/spectronaut/"
        val filePath = resPath + "Gaspari-15321-32-Library_Report.txt"
        val commonRes = colParser!!.parse(filePath, resPath, ResultType.Spectronaut).second
        table = readTableData.getTable(filePath, commonRes.headers)
    }

    @Test
    fun removePotentialContaminants() {
        val params = FilterParams(removePotentialContaminant = true)
        val resTable = runner?.run(table, params, ResultType.Spectronaut.value)
        assert(resTable!!.cols?.get(0)?.size == 7526)
    }

}
