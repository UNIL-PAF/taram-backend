package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.filter.FilterParams
import ch.unil.pafanalysis.annotations.service.AnnotationRepository
import ch.unil.pafanalysis.common.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.sql.Timestamp

@Service
class AsyncOneDEnrichmentRunner() : CommonStep() {

    @Autowired
    val runner: OneDEnrichmentRunner? = null

    @Autowired
    val comp: OneDEnrichmentComputation? = null

    @Autowired
    val annotationRepository: AnnotationRepository? = null

    @Autowired
    private var env: Environment? = null

    private fun getAnnotationPath(): String? {
        return env?.getProperty("result.path.annotations")
    }

    private val readTableData = ReadTableData()
    private val writeEnrichmentTable = WriteEnrichmentTable()


    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            // compute
            val res = computeEnrichment(newStep)

            newStep?.copy(
                results = gson.toJson(res)
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun computeEnrichment(step: AnalysisStep?): OneDEnrichment {
        val resType = step?.analysis?.result?.type
        val outputRoot = getOutputRoot()
        val params = gson.fromJson(step?.parameters, OneDEnrichmentParams().javaClass)
        val table = readTableData.getTable(outputRoot + step?.resultTablePath, step?.commonResult?.headers)
        val annotationFilePath = getAnnotationFilePath(params.annotationId)
        val enrichmentRows = comp?.computeEnrichment(table, resType, params, annotationFilePath)
        val enrichmentTable = saveResToTable(enrichmentRows, step?.resultPath)
        val selEnrichmentRows = getSelEnrichmentRows(enrichmentRows)
        return OneDEnrichment(enrichmentTable, selEnrichmentRows)
    }

    private fun getAnnotationFilePath(annotationId: Int?) : String? {
        val myId = annotationId ?: throw StepException("Could not load annotation [$annotationId].")
        val anno = annotationRepository?.findById(myId)
        return getAnnotationPath() + anno?.fileName
    }

    private fun saveResToTable(enrichmentRows: List<EnrichmentRow>?, resultPath: String?): String? {
        val timestamp = Timestamp(System.currentTimeMillis())
        val fileName = "annotation_table_$timestamp.txt"
        val filePath = getOutputRoot() + resultPath + "/" + fileName
        writeEnrichmentTable.write(filePath, enrichmentRows ?: throw StepException("No enrichments to save."))
        return fileName
    }

    private fun getSelEnrichmentRows(enrichmentRows: List<EnrichmentRow>?, nrRows: Int = 10): List<EnrichmentRow>? {
        return enrichmentRows?.sortedBy { it.pValue }?.take(nrRows)
    }

}