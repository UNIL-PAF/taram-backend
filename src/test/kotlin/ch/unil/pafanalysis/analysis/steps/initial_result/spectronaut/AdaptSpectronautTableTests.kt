package ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File


@SpringBootTest
class AdaptSpectronautTableTests {

    val readTableData = ReadTableData()

    @Autowired
    val columnMappingParser: ColumnMappingParser? = null

    @Test
    fun gaspariTable() {
        val resPath = "./src/test/resources/results/spectronaut/"
        val filePath = resPath + "Gaspari-15321-32-Library_Report.txt"
        val (_, commonRes)  = columnMappingParser?.parse(filePath, resPath, ResultType.Spectronaut)!!

        val resTable = readTableData.getTable(filePath, commonRes?.headers)
        val newTable = AdaptSpectronautTable.adaptTable(resTable)

        assert(resTable?.headers?.size == newTable?.headers?.size)

        val ibaqHeaders = newTable?.headers?.filter{ it.experiment?.field.equals("ibaq", ignoreCase = true) }

        assert(ibaqHeaders?.all { it.type == ColType.NUMBER } == true)
        assert(ibaqHeaders?.size == 12)

        val ibaqCols = newTable?.cols?.filterIndexed{ i, _ -> ibaqHeaders?.map{it.idx}?.contains(i) == true}
        assert(ibaqCols?.size == 12)
        ibaqCols?.forEach{ col -> col.forEach{row ->
            val d = row as? Double
            assert(d != null)
        }}

    }

}
