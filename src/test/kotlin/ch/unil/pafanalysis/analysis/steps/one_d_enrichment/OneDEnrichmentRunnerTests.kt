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
import kotlin.io.path.pathString


@SpringBootTest
class OneDEnrichmentRunnerTests {

    @Autowired
    private val computation: OneDEnrichmentComputation? = null

    @Autowired
    val colParser: ColumnMappingParser? = null


    private val readTableData = ReadTableData()
    private var table: Table? = null

    /*
    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/"
        val filePath = resPath + "Rodrigues_16632-51_Table_8.txt"
        val commonRes = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).second
        val table = readTableData.getTable(filePath, commonRes.headers)
        ints = readTableData.getDoubleMatrix(table, "LFQ.intensity", null).second
    }
     */

    @Test
    fun rodriguesGobpGomfGocc() {
        val resType = ResultType.MaxQuant
        val resTable = readTableData.getTableWithoutHeaders("./src/test/resources/results/maxquant/Rodrigues_16632-51_Table_8.txt")

        val params = OneDEnrichmentParams(
            colName = "fold.change.Ypt7-Ctrl",
            multipleTestCorr = null,
            //categoryNames = listOf("GOCC name", "GOBP name", "GOMF name")
            categoryNames = listOf("GOCC name")
        )

        val annotationFile = "./src/test/resources/annotations/mainAnnot.saccharomyces_cerevisiae_strain_atcc_204508_s288c.txt"
        val enrichmentTable = computation?.computeEnrichment(resTable, resType, params, annotationFile)
        assert(enrichmentTable?.cols?.size == 99)
    }

}
