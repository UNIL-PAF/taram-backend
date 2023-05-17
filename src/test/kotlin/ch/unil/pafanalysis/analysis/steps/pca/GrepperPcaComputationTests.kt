package ch.unil.pafanalysis.analysis.steps.pca

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode

@SpringBootTest
class GrepperPcaComputationTests {

    private val ROUNDING_PRECISION = 4

    @Autowired
    private val runner: PcaComputation? = null

    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()

    private var table: Table? = null
    private var colInfoWithoutGroups: ColumnInfo? = null
    private var colInfo: ColumnInfo? = null
    private var step: AnalysisStep? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = "./src/test/resources/results/maxquant/Grepper_preprocessed.txt"
        val (mqMapping, commonRes) = colParser!!.parse(filePath, resPath, ResultType.MaxQuant)
        table = readTableData.getTable(filePath, commonRes.headers)

        val mqMappingWithGroups = mqMapping.copy(experimentDetails = mqMapping.experimentDetails?.mapValues { (k, v) ->
            if (v.name?.contains("WT") == true) v.copy(group = "WT")
            else v.copy(group = "KO")
        })

        colInfoWithoutGroups = ColumnInfo(columnMapping = mqMapping, columnMappingHash = null)
        colInfo = ColumnInfo(columnMapping = mqMappingWithGroups, columnMappingHash = null)

        step = AnalysisStep(columnInfo = colInfo, commonResult = CommonResult(intColIsLog = true))
    }

    @Test
    fun computePcaWithGroupsTest() {
        val params = PcaParams("LFQ.intensity")
        val pcaRes = runner?.run(table, params, step)!!
        assert(pcaRes.groups?.size == 2)
        assert(pcaRes.nrPc == 16)
        assert(pcaRes.explVars?.size == 16)
        assert(pcaRes.pcList?.size == 16)
        assert(pcaRes.pcList?.get(0)?.expName == "KO-13703")
        assert(pcaRes.pcList?.get(0)?.groupName == "KO")
        assert(roundNumber(pcaRes.pcList?.get(0)?.pcVals?.get(0) ?: 0.0) == roundNumber(-63.790049948674))
        assert(roundNumber(pcaRes.pcList?.get(0)?.pcVals?.get(1) ?: 0.0) == roundNumber(126.092964549002))
        assert(roundNumber(pcaRes.explVars?.get(0) ?: 0.0) == roundNumber(16.1294570663228))
    }

    @Test
    fun computePcaWithoutGroupsTest() {
        val params = PcaParams("LFQ.intensity")
        val pcaRes = runner?.run(table, params, step!!.copy(columnInfo = colInfoWithoutGroups))!!
        assert(pcaRes.groups.isNullOrEmpty())
        assert(pcaRes.nrPc == 16)
        assert(pcaRes.explVars?.size == 16)
        assert(pcaRes.pcList?.size == 16)
        assert(pcaRes.pcList?.get(0)?.expName == "KO-13703")
        assert(pcaRes.pcList?.get(0)?.groupName == null)
        assert(roundNumber(pcaRes.pcList?.get(0)?.pcVals?.get(0) ?: 0.0) == roundNumber(-63.790049948674))
        assert(roundNumber(pcaRes.pcList?.get(0)?.pcVals?.get(1) ?: 0.0) == roundNumber(126.092964549002))
        assert(roundNumber(pcaRes.explVars?.get(0) ?: 0.0) == roundNumber(16.1294570663228))
    }

    private fun roundNumber(n: Double): BigDecimal {
        return BigDecimal(n).setScale(ROUNDING_PRECISION, RoundingMode.HALF_UP)
    }

}
