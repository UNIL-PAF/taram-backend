package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest


@SpringBootTest
class CustomFilterRunnerTests {

    @Autowired
    private val runner: CustomFilterRunner? = null

    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()

    private var table: Table? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val commonRes = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).second
        table = readTableData.getTable(filePath, commonRes.headers)
    }

    @Test
    fun removeNothing() {
        val params = FilterParams()
        val resTable = runner?.run(table, params)
        assert(table!!.cols?.get(0)?.size == 5535)
        assert(resTable!!.cols?.size == table!!.cols?.size)
    }

    @Test
    fun illegalParams1() {
        val colFilter = ColFilter(colName = "Protein.IDs", comparator = Comparator.GE, removeSelected = true, compareToValue = "12")
        val params = FilterParams(colFilters = listOf(colFilter))
        val exception: Exception = assertThrows { runner?.run(table, params) }
        val expectedMessage = "You cannot use the comparator >= on a column containing characters."
        val actualMessage = exception.message
        assert(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun illegalParams2() {
        val colFilter = ColFilter(colName = "Razor.unique.peptides", comparator = Comparator.GE, removeSelected = true, compareToValue = "xxx")
        val params = FilterParams(colFilters = listOf(colFilter))
        val exception: Exception = assertThrows { runner?.run(table, params) }
        val expectedMessage = "You cannot use >= with a non-numeric value."
        val actualMessage = exception.message
        assert(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun FilterUniqePeptides() {
        val colFilter = ColFilter(colName = "Razor.unique.peptides", comparator = Comparator.GT, removeSelected = false, compareToValue = "1")
        val params = FilterParams(colFilters = listOf(colFilter))
        val resTable = runner?.run(table, params)
        assert(resTable!!.cols?.get(0)?.size == 4648)
    }

    @Test
    fun FilterContaminants() {
        val colFilter = ColFilter(colName = "Potential.contaminant", comparator = Comparator.EQ, removeSelected = true, compareToValue = "+")
        val params = FilterParams(colFilters = listOf(colFilter))
        val resTable = runner?.run(table, params)
        assert(resTable!!.cols?.get(0)?.size == 5515)
    }

    @Test
    fun FilterUniqePeptidesAndContaminants() {
        val colFilter = ColFilter(colName = "Razor.unique.peptides", comparator = Comparator.GT, removeSelected = false, compareToValue = "1")
        val colFilter2 = ColFilter(colName = "Potential.contaminant", comparator = Comparator.EQ, removeSelected = true, compareToValue = "+")
        val params = FilterParams(colFilters = listOf(colFilter, colFilter2))
        val resTable = runner?.run(table, params)
        assert(resTable!!.cols?.get(0)?.size == 4635)
    }

}
