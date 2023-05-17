package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal


@SpringBootTest
class ReadTableDataTests {
    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()


    private fun computeMean(col: List<Any>?): Double? {
        val ints = col?.map { it as? Double ?: Double.NaN }?.filter { !it.isNaN() }
        return ints?.average()
    }

    @Test
    fun readFluderTable() {
        val filePath = "./src/test/resources/results/spectronaut/20220707_114227_Fluder-14650-53_Report_Copy.txt"
        val commonRes = colParser!!.parse(filePath, null, ResultType.Spectronaut).second
        val table = readTableData.getTable(filePath, commonRes.headers)

        assert(table!!.cols?.size == 26)
        assert(table!!.cols?.get(0)?.size == 5763)

        assert(table!!.headers!!.isNotEmpty())
        val header = table.headers?.find { it.experiment?.field == "Quantity" && it.experiment?.name == "14650" }

        val quants = table.cols?.get(header!!.idx)
        val mean = BigDecimal(computeMean(quants)!!)

        assert(mean == BigDecimal(10156.141745518113))
    }

    @Test
    fun readMQTable() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val commonRes = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).second
        val table = readTableData.getTable(filePath, commonRes.headers)

        assert(table!!.cols?.size == 219)
        assert(table!!.headers!!.isNotEmpty())
        assert(table!!.headers!!.size == 219)
        assert(table!!.headers!![0].name == "Protein.IDs")

        val header =
            table.headers?.find { it.experiment?.field == "LFQ.intensity" && it.experiment?.name == "WT-13697" }
        val quants = table.cols?.get(header!!.idx)
        val mean = computeMean(quants)?.toInt()
        assert(mean == 789722858)
    }

    @Test
    fun succesfulGetDoubleMatrix() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val commonRes = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).second
        val table = readTableData.getTable(filePath, commonRes.headers)
        val (selHeaders, ints) = readTableData.getDoubleMatrix(table, "LFQ.intensity", null)

        assert(ints?.size == 16)
        assert(selHeaders.size == 16)
        assert(ints[0]?.size == 5535)
        val mean = computeMean(ints[0])?.toInt()
        assert(mean == 762055713)
    }

    @Test
    fun noEntriesExceptionGetDoubleMatrix() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val commonRes = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).second
        val table = readTableData.getTable(filePath, commonRes.headers)

        val exception: Exception = assertThrows { readTableData.getDoubleMatrix(table, "blibla", null) }
        val expectedMessage = "No entries for [blibla] found."
        val actualMessage = exception.message

        assert(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun notNumericalExceptionGetDoubleMatrix() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val commonRes = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).second
        val table = readTableData.getTable(filePath, commonRes.headers)

        val exception: Exception = assertThrows { readTableData.getDoubleMatrix(table, "Identification.type", null) }
        val expectedMessage = "Entries for [Identification.type] are not numerical."
        val actualMessage = exception.message

        assert(actualMessage!!.contains(expectedMessage))
    }

}
