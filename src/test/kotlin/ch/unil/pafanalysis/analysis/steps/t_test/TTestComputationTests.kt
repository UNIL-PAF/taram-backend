package ch.unil.pafanalysis.analysis.steps.t_test

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
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

@SpringBootTest
class TTestComputationTests {

    private val ROUNDING_PRECISION = 2

    @Autowired
    private val runner: TTestComputation? = null

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
    fun computeStudentTest() {
        val params = TTestParams("iBAQ", firstGroup = listOf("KO"), secondGroup = listOf("WT"), equalVariance = true, paired = false)
        val rResPath = "./src/test/resources/results/t_test/student.txt"

        val (resTable, _, _) = runner?.run(table, params, step)!!

        // verify p-values
        val pValHeader = resTable?.headers?.find { it.name?.contains("p.value")?:false }
        val pVals = resTable?.cols?.get(pValHeader?.idx!!)
            ?.map { if (pValHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        val rResults: List<Double> = File(rResPath).readLines()  // Read file line by line
            .mapNotNull { it.toDoubleOrNull() }

        assert(
            roundNumbers(pVals?.filter{!it.isNaN()}) == roundNumbers(rResults.filter{!it.isNaN()})
        )
    }

    @Test
    fun computePairedStudentTest() {
        val params = TTestParams("iBAQ", firstGroup = listOf("KO"), secondGroup = listOf("WT"), equalVariance = true, paired = true)
        val rResPath = "./src/test/resources/results/t_test/paired_student.txt"

        val (resTable, _, _) = runner?.run(table, params, step)!!

        // verify p-values
        val pValHeader = resTable?.headers?.find { it.name?.contains("p.value")?:false }
        val pVals = resTable?.cols?.get(pValHeader?.idx!!)
            ?.map { if (pValHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        val rResults: List<Double> = File(rResPath).readLines()  // Read file line by line
            .mapNotNull { it.toDoubleOrNull() }

        assert(
            roundNumbers(pVals?.filter{!it.isNaN()}) == roundNumbers(rResults.filter{!it.isNaN()})
        )
    }

    @Test
    fun computePairedWelchTest() {
        val params = TTestParams("iBAQ", firstGroup = listOf("KO"), secondGroup = listOf("WT"), equalVariance = false, paired = true)
        val rResPath = "./src/test/resources/results/t_test/paired_welch.txt"

        val (resTable, _, _) = runner?.run(table, params, step)!!

        // verify p-values
        val pValHeader = resTable?.headers?.find { it.name?.contains("p.value")?:false }
        val pVals = resTable?.cols?.get(pValHeader?.idx!!)
            ?.map { if (pValHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        val rResults: List<Double> = File(rResPath).readLines()  // Read file line by line
            .mapNotNull { it.toDoubleOrNull() }

        assert(
            roundNumbers(pVals?.filter{!it.isNaN()}) == roundNumbers(rResults.filter{!it.isNaN()})
        )
    }

    @Test
    fun computeWelchTest() {
        val params = TTestParams("iBAQ", firstGroup = listOf("KO"), secondGroup = listOf("WT"), equalVariance = false, paired = false)
        val rResPath = "./src/test/resources/results/t_test/welch.txt"

        val (resTable, _, _) = runner?.run(table, params, step)!!

        // verify p-values
        val pValHeader = resTable?.headers?.find { it.name?.contains("p.value")?:false }
        val pVals = resTable?.cols?.get(pValHeader?.idx!!)
            ?.map { if (pValHeader.type == ColType.NUMBER) it as? Double ?: Double.NaN else Double.NaN }

        val rResults: List<Double> = File(rResPath).readLines()  // Read file line by line
            .mapNotNull { it.toDoubleOrNull() }

        /*
            TO CHECK RESULTS

            val i = 3543
            println(pVals?.get(i))
            println(roundNumber(pVals?.get(i) ?: 0.0))
            println(rResults[i])
            println(roundNumber(rResults[i]))

            val comp = roundNumbers(pVals?.filter{!it.isNaN()})?.zip(roundNumbers(rResults.filter{!it.isNaN()}) ?: emptyList())
            comp?.forEachIndexed { index, pair ->  if(pair.first != pair.second){println(index); println(pair)} }

         */

        assert(
            roundNumbers(pVals?.filter{!it.isNaN()}) == roundNumbers(rResults.filter{!it.isNaN()})
        )
    }

    private fun roundNumbers(list: List<Double>?): List<Double>? {
        return list?.map { roundNumber(it) }
    }

    private fun roundNumber(n: Double): Double {
        val intermed = String.format("%." + (ROUNDING_PRECISION + 2) + "f", n).toDouble()
        return String.format("%." + ROUNDING_PRECISION + "f", intermed).toDouble()
    }

}
