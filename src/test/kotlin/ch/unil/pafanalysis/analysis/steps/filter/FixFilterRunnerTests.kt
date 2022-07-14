package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.common.ReadTableData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest


@SpringBootTest
class FixFilterRunnerTests {

    @Autowired
    private val runner: FixFilterRunner? = null

    private val readTableData = ReadTableData()

    private var table: ReadTableData.Table? = null

    @BeforeEach
    fun init() {
        val filePath = "./src/test/resources/filter/proteinGroups.txt"
        table = readTableData.getTable(filePath)
    }

    @Test
    fun removeNothing() {
        val params = FilterParams()
        val resTable = runner?.run(table, params)
        assert(table!!.rows?.size == 5535)
        assert(resTable!!.rows?.size == table!!.rows?.size)
    }

    @Test
    fun removeOnlyIdentifiendBySite() {
        val params = FilterParams(removeOnlyIdentifiedBySite = true)
        val resTable = runner?.run(table, params)
        assert(resTable!!.rows?.size == 5457)
    }

    @Test
    fun removeReverse() {
        val params = FilterParams(removeReverse = true)
        val resTable = runner?.run(table, params)
        assert(resTable!!.rows?.size == 5463)
    }

    @Test
    fun removePotentialContaminants() {
        val params = FilterParams(removePotentialContaminant = true)
        val resTable = runner?.run(table, params)
        assert(resTable!!.rows?.size == 5515)
    }

    @Test
    fun removeAll3() {
        val params = FilterParams(removePotentialContaminant = true, removeReverse = true, removeOnlyIdentifiedBySite = true)
        val resTable = runner?.run(table, params)
        assert(resTable!!.rows?.size == 5385)
    }

}
