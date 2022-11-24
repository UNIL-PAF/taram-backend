package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.common.*
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import kotlin.io.path.pathString

@SpringBootTest
class TableServiceTests {

    @Autowired
    val tableService: TableService? = null

    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()
    private val readImputationTable = ReadImputationTableData()

    @Test
    fun replaceImputedVals() {
        val resultPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val resFile = "./src/test/resources/results/maxquant/Grepper_imputed_LFQ_intensity.txt"
        val commonResults = colParser!!.parse(resFile, resultPath, ResultType.MaxQuant).second
        val table = readTableData.getTable(resFile, commonResults.headers)

        val fileName = "./src/test/resources/results/maxquant/Grepper_imputation.txt"
        val imputationTable = readImputationTable.getTable(fileName, commonResults.headers)

        val (newTable, nrReplaced, nrRowsReplaced) = tableService?.replaceImputedVals(table, imputationTable, Double.NaN)!!

        val tempFile = kotlin.io.path.createTempFile()
        writeTableData.write(tempFile.pathString, newTable!!)
        val fileHash = Crc32HashComputations().computeFileHash(File(fileName))

        println(tempFile.pathString)

        assert(fileHash == (1653853479).toLong())
        assert(nrReplaced == 10231)
        assert(nrRowsReplaced == 1779)
    }
}