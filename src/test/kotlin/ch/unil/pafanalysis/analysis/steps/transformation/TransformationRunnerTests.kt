package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.common.*
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.io.path.pathString


@SpringBootTest
class TransformationRunnerTests {

    @Autowired
    private val imputation: ImputationRunner? = null

    @Autowired
    private val normalization: NormalizationRunner? = null

    @Autowired
    private val transformation: LogTransformationComputation? = null

    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    private var ints: List<List<Double>>? = null
    private var table: Table? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val commonRes = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).second
        table = readTableData.getTable(filePath, commonRes.headers)
        ints = readTableData.getDoubleMatrix(table, "LFQ.intensity").second
    }

    @Test
    fun defaultTransformationChain() {
        val params = TransformationParams(
            transformationType = TransformationType.LOG2.value,
            normalizationType = NormalizationType.MEDIAN.value,
            imputationType = ImputationType.NORMAL.value,
            imputationParams = ImputationParams()
        )
        val res1 = transformation?.runTransformation(ints!!, params)
        val res2 = normalization?.runNormalization(res1!!, params)
        val res3 = imputation?.runImputation(res2!!, params)
        val oneRes = BigDecimal(res3!!.first[0][22]).setScale(5, RoundingMode.HALF_EVEN).toDouble()

        assert(ints!![0][22] == 0.0)
        assert(oneRes == -4.55469)
    }

    @Test
    fun writeTransformationChain(){
        val params = TransformationParams(
            transformationType = TransformationType.LOG2.value,
            normalizationType = NormalizationType.MEDIAN.value,
            imputationType = ImputationType.NORMAL.value,
            imputationParams = ImputationParams()
        )
        val res1 = transformation?.runTransformation(ints!!, params)
        val res2 = normalization?.runNormalization(res1!!, params)
        val res3 = imputation?.runImputation(res2!!, params)?.first

        val selHeaders = readTableData.getDoubleMatrix(table, "LFQ.intensity").first

        val newCols: List<List<Any>>? = table?.cols?.mapIndexed{ i, c ->
            val selHeader = selHeaders.withIndex().find{ it.value.idx == i }
            if (selHeader != null) {
                res3!![selHeader.index]
            }else c
        }
        val tempFile = kotlin.io.path.createTempFile()
        val fileName = writeTableData.write(tempFile.pathString, table!!.copy(cols = newCols))
        val fileHash = Crc32HashComputations().computeFileHash(File(fileName))
        assert(fileHash == (458642479).toLong())
    }

}
