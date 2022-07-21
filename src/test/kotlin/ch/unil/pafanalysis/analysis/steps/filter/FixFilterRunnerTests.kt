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
class FixFilterRunnerTests {

    @Autowired
    private val runner: FixFilterRunner? = null

    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()

    private var table: Table? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val mqMapping = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).first
        table = readTableData.getTable(filePath, mqMapping)
    }

    @Test
    fun removeNothing() {
        val params = FilterParams()
        val resTable = runner?.run(table, params, null)
        assert(table!!.cols?.get(0)?.size == 5535)
        assert(resTable!!.cols?.size == table!!.cols?.size)
    }

    @Test
    fun removeOnlyIdentifiendBySite() {
        val params = FilterParams(removeOnlyIdentifiedBySite = true)
        val resTable = runner?.run(table, params, null)
        assert(resTable!!.cols?.get(0)?.size == 5457)
    }

    @Test
    fun removeReverse() {
        val params = FilterParams(removeReverse = true)
        val resTable = runner?.run(table, params, null)
        assert(resTable!!.cols?.get(0)?.size == 5463)
    }

    @Test
    fun removePotentialContaminants() {
        val params = FilterParams(removePotentialContaminant = true)
        val resTable = runner?.run(table, params, null)
        assert(resTable!!.cols?.get(0)?.size == 5515)
    }

    @Test
    fun removeAll3() {
        val params = FilterParams(removePotentialContaminant = true, removeReverse = true, removeOnlyIdentifiedBySite = true)
        val resTable = runner?.run(table, params, null)
        assert(resTable!!.cols?.get(0)?.size == 5385)
    }

}
