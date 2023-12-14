package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.analysis.steps.imputation.ImputationComputation
import ch.unil.pafanalysis.analysis.steps.imputation.ImputationParams
import ch.unil.pafanalysis.analysis.steps.imputation.ImputationType
import ch.unil.pafanalysis.analysis.steps.imputation.NormImputationParams
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.io.path.pathString


@SpringBootTest
class OneDEnrichmentRunnerTests {

    @Autowired
    private val computation: OneDEnrichmentComputation? = null
    private val readTableData = ReadTableData()


    private val roundingPrecision = 7

    private fun roundNumber(n: Double?, precision: Int = roundingPrecision): BigDecimal {
        return if(n == null) BigDecimal(0)
        else BigDecimal(n).setScale(precision, RoundingMode.HALF_UP)
    }

    @Test
    fun rodriguesGobpGomfGocc() {
        val resType = ResultType.MaxQuant.value
        val resTable = readTableData.getTableWithoutHeaders("./src/test/resources/results/maxquant/Rodrigues_16632-51_Table_8.txt")

        val params = OneDEnrichmentParams(
            colIdx = 201, //"fold.change.Ypt7-Ctrl",
            fdrCorrection = true,
            categoryIds = listOf(1, 2, 3),
            threshold = 0.02
        )

        val annotationFile = "./src/test/resources/annotations/mainAnnot.saccharomyces_cerevisiae_strain_atcc_204508_s288c.txt"
        //val annotationFile = "/Users/rmylonas/Work/PAF/projects/paf-analysis/data/annotations/mainAnnot.homo_sapiens.txt"
        val enrichmentRes = computation?.computeEnrichment(resTable, resType, params, listOf("GOCC name", "GOBP name", "GOMF name"), annotationFile)


        assert(enrichmentRes?.size == 412)
        assert(enrichmentRes?.filter{it.type == "GOCC name"}?.size == 98)
        val selRes = enrichmentRes?.find{it.name == "membrane part"}

        assert(selRes?.column == "fold.change.Ypt7-Ctrl")
        assert(selRes?.type == "GOCC name")
        assert(selRes?.name == "membrane part")
        assert(selRes?.size == 1058)
        assert(roundNumber(selRes?.score) == roundNumber(0.41310526669970205))
        assert(roundNumber(selRes?.pvalue) == roundNumber(9.98346216121655E-90))
        assert(roundNumber(selRes?.qvalue) == roundNumber(7.57744778036336E-87))
        assert(roundNumber(selRes?.mean) == roundNumber(0.704807648038113))
        assert(roundNumber(selRes?.median) == roundNumber(0.539801459001287))
    }

    /*

    @Test
    fun bernaleau() {
        val resType = ResultType.MaxQuant.value
        val resTable = readTableData.getTableWithoutHeaders("/Users/rmylonas/Work/PAF/projects/paf-analysis/data/1D_annotation_examples/Bernaleau_16695-98-825-28/Table-10.txt")

        val params = OneDEnrichmentParams(
            colIdx = 105, //"fold.change.Ypt7-Ctrl",
            fdrCorrection = true,
            categoryIds = listOf(1, 2, 3),
            threshold = 0.02
        )

        val annotationFile = "/Users/rmylonas/Work/PAF/projects/paf-analysis/data/1D_annotation_examples/Bernaleau_16695-98-825-28/mainAnnot.homo_sapiens.txt"
        //val annotationFile = "/Users/rmylonas/Work/PAF/projects/paf-analysis/data/annotations/mainAnnot.homo_sapiens.txt"
        val enrichmentRes = computation?.computeEnrichment(resTable, resType, params, listOf("GOCC name", "GOBP name", "GOMF name", "KEGG name"), annotationFile)

        assert(enrichmentRes?.size == 31)
        assert(enrichmentRes?.filter{it.type == "KEGG name"}?.size == 2)

        val selRes = enrichmentRes?.find{it.name == "endoplasmic reticulum part"}
        assert(selRes?.column == "fold.change.CCDC134-Ev")
        assert(selRes?.type == "GOCC name")
        assert(selRes?.size == 45)
        assert(roundNumber(selRes?.score) == roundNumber(0.610546139359699))
        assert(roundNumber(selRes?.pvalue) == roundNumber(1.00866844473792E-11))
        assert(roundNumber(selRes?.qvalue) == roundNumber(5.84019029503255E-09))
        assert(roundNumber(selRes?.mean) == roundNumber(2.076135029))
        assert(roundNumber(selRes?.median) == roundNumber(2.080982152))
    }

     */

}
