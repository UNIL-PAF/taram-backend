package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.analysis.steps.imputation.ImputationComputation
import ch.unil.pafanalysis.analysis.steps.imputation.ImputationParams
import ch.unil.pafanalysis.analysis.steps.imputation.ImputationType
import ch.unil.pafanalysis.analysis.steps.imputation.NormImputationParams
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.io.path.pathString


@SpringBootTest
class WriteImputationTableDataTests {
    @Autowired
    val colParser: ColumnMappingParser? = null

    @Autowired
    val imputationRunner: ImputationComputation? = null

    private val readTableData = ReadTableData()
    private val writeImputationTable = WriteImputationTableData()
    private val readImputationTable = ReadImputationTableData()
    private var headers: List<Header>? = null


    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val commonRes = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).second
        headers = commonRes.headers
    }

    @Test
    fun writeGrepperImputationTable() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val table = readTableData.getTable(filePath, headers)
        val (cols, ints) = readTableData.getDoubleMatrix(table, "LFQ.intensity")

        assert(table!!.cols?.size == 219)
        assert(table!!.headers!!.isNotEmpty())

        val imputParams = NormImputationParams(width = 0.5, downshift = 2.0, seed = 10)
        val params = ImputationParams(imputationType = ImputationType.NORMAL.value, normImputationParams = imputParams)
        val imputationMatrix = imputationRunner?.runImputation(ints, params)?.second
        val imputationTable = ImputationTable(cols, imputationMatrix)

        val tempFile = createTempFile()
        val filename = writeImputationTable.write(tempFile.pathString, imputationTable)
        assert(File(filename).exists())
        val fileHash = Crc32HashComputations().computeFileHash(File(filename))
        assert(fileHash == (1653853479).toLong())
    }

    @Test
    fun readGrepperImputationTable() {
        val fileName = "./src/test/resources/results/maxquant/Grepper_imputation.txt"
        val imputationTable = readImputationTable.getTable(fileName, headers)
        assert(imputationTable.headers?.size == 16)
        val expectedRow = listOf(
            false,
            false,
            true,
            true,
            false,
            true,
            false,
            false,
            false,
            true,
            false,
            false,
            false,
            false,
            false,
            false
        )
        assert(imputationTable.rows?.get(1) == expectedRow)
    }

}
