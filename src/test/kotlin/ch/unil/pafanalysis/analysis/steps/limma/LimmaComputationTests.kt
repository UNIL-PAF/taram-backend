package ch.unil.pafanalysis.analysis.steps.limma

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.common.WriteTableData
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import kotlin.math.sign

@SpringBootTest
class LimmaComputationTests {

    private val ROUNDING_PRECISION = 2

    @Autowired
    private val runner: LimmaComputation? = null

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
    fun computeLimma() {
        val params = LimmaParams("LFQ.intensity", firstGroup = listOf("KO"), secondGroup = listOf("WT"), paired = false)
        val rResPath = "./src/test/resources/results/limma/Grepper_preprocessed.txt"

        val (resTable, limma) = runner?.run(table, params, step)!!

        assert(limma.comparisions?.first()?.numberOfSignificant == 102)

        val rTable = readResultsFromR(rResPath)
        assertCol("p.value", rTable, resTable!!)
        assertCol("adj.p.value", rTable, resTable)
        assertCol("log2.fold.change", rTable, resTable)
        assertCol("t.statistic", rTable, resTable)

        val colname = "is.significant"
        val signHeaderR = rTable.headers?.find { it.name?.contains(colname)?:false }
        val signValsR = ReadTableData().getStringColumn(rTable, signHeaderR?.name!!)
        assert(signValsR?.filter{it == "TRUE"}?.size == 102)
    }

    private fun assertCol(colname: String, rTable:Table, resTable: Table){
        val pValHeaderR = rTable.headers?.find { it.name?.contains(colname)?:false }
        val rPVals = ReadTableData().getDoubleColumn(rTable, pValHeaderR?.name!!)
        val pValHeader = resTable.headers?.find { it.name?.contains(colname)?:false }
        val pVals = ReadTableData().getDoubleColumn(rTable, pValHeader?.name!!)

        assert(
            roundNumbers(pVals?.filter{!it.isNaN()}) == roundNumbers(rPVals?.filter{!it.isNaN()})
        )
    }


    private fun readResultsFromR(filePath: String): Table{

        val headers = listOf(
            Header("p.value.KO-WT", 0, ColType.NUMBER),
            Header("adj.p.value.KO-WT", 1, ColType.NUMBER),
            Header("log2.fold.change.KO-WT", 2, ColType.NUMBER),
            Header("is.significant.KO-WT", 3, ColType.CHARACTER),
            Header("t.statistic.KO-WT", 4, ColType.NUMBER),
        )
        val table = ReadTableData().getTable(filePath, headers)
        return table
    }

    private fun roundNumbers(list: List<Double>?): List<Double>? {
        return list?.map { roundNumber(it) }
    }

    private fun roundNumber(n: Double): Double {
        //val intermed = String.format("%." + (ROUNDING_PRECISION + 2) + "f", n).toDouble()
        return String.format("%." + ROUNDING_PRECISION + "f", n).toDouble()
    }

}
