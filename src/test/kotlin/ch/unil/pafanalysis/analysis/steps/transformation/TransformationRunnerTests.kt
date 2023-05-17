package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.analysis.steps.imputation.ImputationComputation
import ch.unil.pafanalysis.analysis.steps.imputation.ImputationParams
import ch.unil.pafanalysis.analysis.steps.imputation.ImputationType
import ch.unil.pafanalysis.analysis.steps.imputation.NormImputationParams
import ch.unil.pafanalysis.analysis.steps.log_transformation.LogTransformationComputation
import ch.unil.pafanalysis.analysis.steps.log_transformation.LogTransformationParams
import ch.unil.pafanalysis.analysis.steps.log_transformation.TransformationType
import ch.unil.pafanalysis.analysis.steps.normalization.NormalizationComputation
import ch.unil.pafanalysis.analysis.steps.normalization.NormalizationParams
import ch.unil.pafanalysis.analysis.steps.normalization.NormalizationType
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
    private val imputation: ImputationComputation? = null

    @Autowired
    private val normalization: NormalizationComputation? = null

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
        ints = readTableData.getDoubleMatrix(table, "LFQ.intensity", null).second
    }

    @Test
    fun defaultTransformationChain() {
        val transParams = LogTransformationParams(transformationType = TransformationType.LOG2.value)
        val res1 = transformation?.runTransformation(ints!!, transParams)

        val normParams = NormalizationParams(normalizationType = NormalizationType.MEDIAN.value)
        val res2 = normalization?.runNormalization(res1!!, normParams)

        val imputParams = ImputationParams(imputationType = ImputationType.NORMAL.value, normImputationParams = NormImputationParams())
        val res3 = imputation?.runImputation(res2!!, imputParams)
        val oneRes = BigDecimal(res3!!.first[0][22]).setScale(5, RoundingMode.HALF_EVEN).toDouble()

        assert(ints!![0][22] == 0.0)
        assert(oneRes == -4.55469)
    }

    @Test
    fun writeTransformationChain(){
        val transParams = LogTransformationParams(transformationType = TransformationType.LOG2.value)
        val res1 = transformation?.runTransformation(ints!!, transParams)

        val normParams = NormalizationParams(normalizationType = NormalizationType.MEDIAN.value)
        val res2 = normalization?.runNormalization(res1!!, normParams)

        val imputParams = ImputationParams(imputationType = ImputationType.NORMAL.value, normImputationParams = NormImputationParams())
        val res3 = imputation?.runImputation(res2!!, imputParams)?.first
        val oneRes = BigDecimal(res3!![0][22]).setScale(5, RoundingMode.HALF_EVEN).toDouble()

        val selHeaders = readTableData.getDoubleMatrix(table, "LFQ.intensity", null).first

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
