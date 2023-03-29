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
    fun parseMaxQuantBernard() {
        val resultPath = "./src/test/resources/results/maxquant/Bernard_exp_2617/"
        val resFile = resultPath + "proteinGroups.txt"
        val (colMapping, commonResults) = colParser!!.parse(resFile, resultPath, ResultType.MaxQuant)

        assert(colMapping.experimentNames?.size == 4)
        assert(colMapping.experimentNames!!.contains("15628"))
        assert(colMapping.intCol == "Intensity")
        assert(commonResults.headers?.size == 64)

        // check first header
        assert(commonResults.headers?.get(0)?.idx == 0)
        assert(commonResults.headers?.get(0)?.name == "Protein.IDs")
        assert(commonResults.headers?.get(0)?.type == ColType.CHARACTER)

        // check commonResults
        assert(commonResults.numericalColumns?.size == 6)
        assert(commonResults.numericalColumns!!.contains("iBAQ"))
    }

    @Test
    fun parseMaxQuantGrepper() {
        val resultPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val resFile = resultPath + "proteinGroups.txt"
        val (colMapping, commonResults) = colParser!!.parse(resFile, resultPath, ResultType.MaxQuant)

        assert(colMapping.experimentNames?.size == 16)
        assert(colMapping.experimentNames!!.contains("KO-13703"))
        assert(colMapping.intCol == "Intensity")
        assert(commonResults.headers?.size == 219)

        // check first header
        assert(commonResults.headers?.get(0)?.idx == 0)
        assert(commonResults.headers?.get(0)?.name == "Protein.IDs")
        assert(commonResults.headers?.get(0)?.type == ColType.CHARACTER)

        // check header with experiment
        assert(commonResults.headers?.get(12)?.idx == 12)
        assert(commonResults.headers?.get(12)?.name == "KO-13704.Mutation.names")
        assert(commonResults.headers?.get(12)?.type == ColType.NUMBER)
        assert(commonResults.headers?.get(12)?.experiment?.name == "KO-13704")
        assert(commonResults.headers?.get(12)?.experiment?.field == "Mutation.names")

        // check commonResults
        assert(commonResults.numericalColumns?.size == 10)
        assert(commonResults.numericalColumns!!.contains("LFQ.intensity"))
    }

    @Test
    fun parseSpectronautFluder() {
        val resFile = "./src/test/resources/results/spectronaut/20220707_114227_Fluder-14650-53_Report_Copy.txt"
        val (colMapping, commonResults) = colParser!!.parse(resFile, null, ResultType.Spectronaut)
        assert(colMapping.experimentNames?.size == 4)
        assert(colMapping.experimentNames!!.contains("14650"))
        assert(colMapping.intCol == "Quantity")
        assert(commonResults.headers?.size == 26)

        // check first header
        assert(commonResults.headers?.get(0)?.idx == 0)
        assert(commonResults.headers?.get(0)?.name == "PG.ProteinGroups")
        assert(commonResults.headers?.get(0)?.type == ColType.CHARACTER)

        // check header with experiment
        assert(commonResults.headers?.get(12)?.idx == 12)
        assert(commonResults.headers?.get(12)?.name == "14652.NrOfPrecursorsIdentified")
        assert(commonResults.headers?.get(12)?.type == ColType.NUMBER)
        assert(commonResults.headers?.get(12)?.experiment?.name == "14652")
        assert(commonResults.headers?.get(12)?.experiment?.field == "NrOfPrecursorsIdentified")

        // check commonResults
        assert(commonResults.numericalColumns?.size == 3)
        assert(commonResults.numericalColumns!!.contains("NrOfPrecursorsIdentified"))
    }

    @Test
    fun parseSpectronautGutierrez() {
        val resFile = "./src/test/resources/results/spectronaut/20220225_171348_Gutierrez-14180-204_Report_Copy.txt"
        val (colMapping, commonResults) = colParser!!.parse(resFile, null, ResultType.Spectronaut)
        assert(colMapping.experimentNames?.size == 25)
        assert(colMapping.experimentNames!!.contains("14204"))
        assert(colMapping.intCol == "Quantity")
        assert(commonResults.headers?.size == 85)

        // check first header
        assert(commonResults.headers?.get(0)?.idx == 0)
        assert(commonResults.headers?.get(0)?.name == "PG.MolecularWeight")
        assert(commonResults.headers?.get(0)?.type == ColType.CHARACTER)

        // check header with experiment
        assert(commonResults.headers?.get(11)?.idx == 11)
        assert(commonResults.headers?.get(11)?.name == "14181.NrOfPrecursorsIdentified")
        assert(commonResults.headers?.get(11)?.type == ColType.NUMBER)
        assert(commonResults.headers?.get(11)?.experiment?.name == "14181")
        assert(commonResults.headers?.get(11)?.experiment?.field == "NrOfPrecursorsIdentified")

        // check commonResults
        assert(commonResults.numericalColumns?.size == 3)
        assert(commonResults.numericalColumns!!.contains("NrOfPrecursorsIdentified"))
    }

    @Test
    fun parseSpectronautZanou() {
        val resFile = "./src/test/resources/results/spectronaut/20211220_142703_Zanou-DIA-12811-22_Peptide_Report_Copy.txt"
        val (colMapping, commonResults) = colParser!!.parse(resFile, null, ResultType.Spectronaut)

        assert(colMapping.experimentNames?.size == 12)
        assert(colMapping.experimentNames!!.contains("12817"))
        assert(colMapping.intCol == null)
        assert(commonResults.headers?.size == 19)

        // check first header
        assert(commonResults.headers?.get(0)?.idx == 0)
        assert(commonResults.headers?.get(0)?.name == "PG.Qvalue")
        assert(commonResults.headers?.get(0)?.type == ColType.NUMBER)

        // check header with experiment
        assert(commonResults.headers?.get(11)?.idx == 11)
        assert(commonResults.headers?.get(11)?.name == "12818.TotalQuantity (Settings)")
        assert(commonResults.headers?.get(11)?.type == ColType.NUMBER)
        assert(commonResults.headers?.get(11)?.experiment?.name == "12818")
        assert(commonResults.headers?.get(11)?.experiment?.field == "TotalQuantity (Settings)")

        // check commonResults
        assert(commonResults.numericalColumns?.size == 1)
        assert(commonResults.numericalColumns!!.contains("TotalQuantity (Settings)"))
    }
}