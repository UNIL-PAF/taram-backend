package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.volcano.VolcanoPlotParams
import ch.unil.pafanalysis.annotations.service.AnnotationRepository
import ch.unil.pafanalysis.common.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

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
    private val writeEnrichmentTable = EnrichmentTableWriter()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val nrRows = 10
            val params = gson.fromJson(newStep?.parameters, OneDEnrichmentParams().javaClass)

            // compute
            val res = computeEnrichment(newStep, params, nrRows)
            val newParams = addSelResults(params, nrRows)

            newStep?.copy(
                results = gson.toJson(res),
                parameters = gson.toJson(newParams)
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun computeEnrichment(step: AnalysisStep?, params: OneDEnrichmentParams, nrRows: Int): OneDEnrichment {
        val resType = step?.analysis?.result?.type
        val outputRoot = getOutputRoot()

        val table = readTableData.getTable(outputRoot + step?.resultTablePath, step?.commonResult?.headers)
        val annotationFilePath = getAnnotationFilePath(params.annotationId)
        val enrichmentRows = comp?.computeEnrichment(table, resType, params, annotationFilePath)
        val enrichmentTable = saveResToTable(enrichmentRows, step?.resultPath)
        val selEnrichmentRows = getSelEnrichmentRows(enrichmentRows, nrRows)
        return OneDEnrichment(enrichmentTable, selEnrichmentRows)
    }

    private fun addSelResults(params: OneDEnrichmentParams, nrRows: Int): OneDEnrichmentParams {
        val selRes = (0 until nrRows).toList()
        return params.copy(selResults = selRes)
    }

    private fun getAnnotationFilePath(annotationId: Int?): String? {
        val myId = annotationId ?: throw StepException("Could not load annotation [$annotationId].")
        val anno = annotationRepository?.findById(myId)
        return getAnnotationPath() + anno?.fileName
    }

    private fun saveResToTable(enrichmentRows: List<EnrichmentRow>?, resultPath: String?): String? {
        val currentDateTime: java.util.Date = java.util.Date()
        val currentTimestamp: Long = currentDateTime.time
        val fileName = "enrichment_table_$currentTimestamp.txt"
        val filePath = getOutputRoot() + resultPath + "/" + fileName
        writeEnrichmentTable.write(filePath, enrichmentRows ?: throw StepException("No enrichment to save."))
        return fileName
    }

    private fun getSelEnrichmentRows(enrichmentRows: List<EnrichmentRow>?, nrRows: Int): List<EnrichmentRow>? {
        return enrichmentRows?.sortedBy { it.pValue }?.mapIndexed { i, row -> row.copy(id = i) }?.take(nrRows)
    }

}