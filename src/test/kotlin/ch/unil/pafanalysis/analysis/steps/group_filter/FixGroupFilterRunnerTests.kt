package ch.unil.pafanalysis.analysis.steps.group_filter

import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest


@SpringBootTest
class FixFilterRunnerTests {

    @Autowired
    private val runner: FixGroupFilterRunner? = null

    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()

    private var table: Table? = null
    private var colInfoWithoutGroups: ColumnInfo? = null
    private var colInfo: ColumnInfo? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val mqMapping = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).first
        val tableWithoutGroups = readTableData.getTable(filePath, mqMapping)

        val mqMappingWithGroups = mqMapping.copy(experimentDetails = mqMapping.experimentDetails?.mapValues { (k, v) ->
            if (v.name?.contains("WT") == true) v.copy(group = "WT")
            else v.copy(group = "KO")
        })

        table = tableWithoutGroups.copy(headers = tableWithoutGroups.headers?.map {
            it.copy(
                experiment = it.experiment?.copy(group = if (it.experiment?.name?.contains("WT") == true) "WT" else "KO")
            )
        })

        colInfoWithoutGroups = ColumnInfo(columnMapping = mqMapping, columnMappingHash = null)
        colInfo = ColumnInfo(columnMapping = mqMappingWithGroups, columnMappingHash = null)
    }

    @Test
    fun checkUndefinedGroupException() {
        val params = GroupFilterParams(8, FilterInGroup.ONE_GROUP, "Intensity")

        val exception: Exception = assertThrows { runner?.run(table, params, colInfoWithoutGroups) }
        val expectedMessage = "Please specify your groups in the 'Initial result' parameters."
        val actualMessage = exception.message

        println(actualMessage)

        assert(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun checkOneGroup() {
        val params = GroupFilterParams(5, FilterInGroup.ONE_GROUP, "LFQ.intensity")
        val resTable = runner?.run(table, params, colInfo)
        assert(table!!.cols?.get(0)?.size == 5535)
        assert(resTable!!.cols?.get(0)?.size == 5074)
    }


    @Test
    fun checkAllGroups() {
        val params = GroupFilterParams(5, FilterInGroup.ALL_GROUPS, "LFQ.intensity")
        val resTable = runner?.run(table, params, colInfo)
        assert(table!!.cols?.get(0)?.size == 5535)
        assert(resTable!!.cols?.get(0)?.size == 4761)
    }

}
