package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.analysis.steps.imputation.ImputationComputation
import ch.unil.pafanalysis.analysis.steps.imputation.ImputationParams
import ch.unil.pafanalysis.analysis.steps.imputation.ImputationType
import ch.unil.pafanalysis.analysis.steps.imputation.NormImputationParams
import ch.unil.pafanalysis.annotations.model.AnnotationInfo
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
class AnnotationColumnAddTests {

    private val readTableData = ReadTableData()

    @Test
    fun rodriguesOneCol() {
        val resType = ResultType.MaxQuant.value
        val resTable = readTableData.getTableWithoutHeaders("./src/test/resources/results/maxquant/Rodrigues_16632-51_Table_8.txt")

        val params = OneDEnrichmentParams(
            colIdxs = listOf(201), //"fold.change.Ypt7-Ctrl",
            fdrCorrection = true,
            categoryIds = listOf(1, 2, 3),
            threshold = 0.02
        )

        val annotationFile = "./src/test/resources/annotations/mainAnnot.saccharomyces_cerevisiae_strain_atcc_204508_s288c.txt"
        val newTable = AnnotationColumnAdd.addAnnotations(resTable, resType, annotationFile, params)

        assert(newTable?.cols?.size == resTable?.cols?.size?.plus(3))
        assert(newTable?.cols?.size == 210)

        assert(newTable?.headers?.size == resTable?.headers?.size?.plus(3))
        assert(newTable?.headers?.size == 210)

        val selHeader = newTable?.headers?.find{ it.name == "GOBP name" }
        assert(selHeader?.idx == 207)
        assert(selHeader?.type == ColType.CHARACTER)

        val selHeader2 = newTable?.headers?.find{ it.name == "GOMF name" }
        assert(selHeader2?.idx == 208)

        val selEntry = newTable?.cols?.get(207)?.get(0).toString()
        assert(selEntry == "pheromone-dependent signal transduction involved in conjugation with cellular fusion;cytogamy;protein complex assembly;signal transduction;cell surface receptor linked signaling pathway;G-protein coupled receptor protein signaling pathway;cellular process;cellular component organization;reproductive process in single-celled organism;reproductive process;cellular component assembly;cell projection organization;cell projection assembly;signal transduction involved in conjugation with cellular fusion;macromolecular complex subunit organization;cellular process involved in reproduction;regulation of biological process;regulation of cellular process;response to stimulus;protein oligomerization;protein homooligomerization;cellular response to stimulus;macromolecular complex assembly;biological regulation;protein complex subunit organization;cellular component organization or biogenesis;cellular component organization or biogenesis at cellular level;cellular component organization at cellular level;cellular component assembly at cellular level")
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
