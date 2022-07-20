package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ColumnMappingParserTests {

    @Autowired
    val colParser: ColumnMappingParser? = null

    @Test
    fun parseSpectronautFluder() {
        val resFile = "./src/test/resources/results/spectronaut/20220707_114227_Fluder-14650-53_Report_Copy.txt"
        val (colMapping, commonResults) = colParser!!.parse(resFile, null, ResultType.Spectronaut)
        assert(colMapping.intColumn == "Quantity")
        assert(colMapping.experimentNames?.size == 4)
        assert(colMapping.experimentNames!!.contains("14650"))
        assert(colMapping.headers?.size == 26)

        // check first header
        assert(colMapping.headers?.get(0)?.idx == 0)
        assert(colMapping.headers?.get(0)?.name == "PG.ProteinGroups")
        assert(colMapping.headers?.get(0)?.type == ColType.CHARACTER)

        // check header with experiment
        assert(colMapping.headers?.get(12)?.idx == 12)
        assert(colMapping.headers?.get(12)?.name == "14652.NrOfPrecursorsIdentified")
        assert(colMapping.headers?.get(12)?.type == ColType.NUMBER)
        assert(colMapping.headers?.get(12)?.experiment?.name == "14652")
        assert(colMapping.headers?.get(12)?.experiment?.field == "NrOfPrecursorsIdentified")

        // check commonResults
        assert(commonResults.intCol == "Quantity")
        assert(commonResults.numericalColumns?.size == 3)
        assert(commonResults.numericalColumns!!.contains("NrOfPrecursorsIdentified"))
    }
}