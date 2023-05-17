package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.analysis.steps.log_transformation.LogTransformationComputation
import ch.unil.pafanalysis.analysis.steps.log_transformation.LogTransformationParams
import ch.unil.pafanalysis.analysis.steps.log_transformation.TransformationType
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode


@SpringBootTest
class LogTransformationRunnerTests {

    @Autowired
    private val runner: LogTransformationComputation? = null

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
    fun log2Transformation() {
        val params = LogTransformationParams(transformationType = TransformationType.LOG2.value)
        val ints = readTableData.getDoubleMatrix(table, "Intensity", null).second

        val res = runner?.runTransformation(ints!!, params)
        val oneRes = BigDecimal(res!![0][0]).setScale(5, RoundingMode.HALF_EVEN)
        assert(oneRes.toDouble() == 23.98094)
    }

    @Test
    fun noneTransformation() {
        val params = LogTransformationParams(transformationType = TransformationType.NONE.value)
        val ints = readTableData.getDoubleMatrix(table, "Intensity", null).second
        val res = runner?.runTransformation(ints!!, params)
        val oneRes = res!![0][0]
        assert(oneRes == 16557000.0)
    }


}
