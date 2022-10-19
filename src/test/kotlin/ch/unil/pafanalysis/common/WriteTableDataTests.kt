package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import kotlin.io.path.createTempFile
import java.math.BigDecimal
import kotlin.io.path.pathString


@SpringBootTest
class WriteTableDataTests {
    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    @Test
    fun readAndWriteFluderTable() {
        val filePath = "./src/test/resources/results/spectronaut/20220707_114227_Fluder-14650-53_Report_Copy.txt"
        val commonRes = colParser!!.parse(filePath, null, ResultType.Spectronaut).second
        val table = readTableData.getTable(filePath, commonRes.headers)

        assert(table!!.cols?.size == 26)
        assert(table!!.cols?.get(0)?.size == 5763)

        val tempFile = createTempFile()
        val filename = writeTableData.write(tempFile.pathString, table)
        assert(File(filename).exists())
        val fileHash = Crc32HashComputations().computeFileHash(File(filename))
        assert(fileHash == (317433011).toLong())
    }

    @Test
    fun readAndWriteMQTable() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val commonRes = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).second
        val table = readTableData.getTable(filePath, commonRes.headers)

        assert(table!!.cols?.size == 219)
        assert(table!!.headers!!.isNotEmpty())

        val tempFile = createTempFile()
        val filename = writeTableData.write(tempFile.pathString, table)
        assert(File(filename).exists())
        val fileHash = Crc32HashComputations().computeFileHash(File(filename))
        println(fileHash)
        assert(fileHash == (2818018004).toLong())
    }

}
