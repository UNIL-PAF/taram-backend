package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.annotations.model.AnnotationInfo
import ch.unil.pafanalysis.annotations.service.AnnotationRepository
import ch.unil.pafanalysis.annotations.service.AnnotationService
import ch.unil.pafanalysis.common.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service


@Service
class AsyncOneDEnrichmentRunner() : CommonStep() {

    var logger: Logger = LoggerFactory.getLogger(AsyncOneDEnrichmentRunner::class.java)

    @Autowired
    val runner: OneDEnrichmentRunner? = null

    @Autowired
    val comp: OneDEnrichmentComputation? = null

    @Autowired
    val annotationRepository: AnnotationRepository? = null

    @Autowired
    val annotationService: AnnotationService? = null

    @Autowired
    private var env: Environment? = null

    private val writeTableData = WriteTableData()

    private fun getAnnotationPath(anno: AnnotationInfo?): String? {
        return env?.getProperty("result.path.annotations") + anno?.fileName
    }

    private val readTableData = ReadTableData()
    private val writeEnrichmentTable = EnrichmentTableWriter()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val nrRows = 10
            val params = gson.fromJson(newStep?.parameters, OneDEnrichmentParams().javaClass)

            val annoId = params.annotationId ?: throw StepException("Could not load annotation [$params.annotationId].")
            val anno = annotationRepository?.findById(annoId)

            val outputRoot = getOutputRoot()
            val table: Table = readTableData.getTable(outputRoot + newStep?.resultTablePath, newStep?.commonResult?.headers)

            // compute
            logger.info("1D-enrichment: computeEnrichment.")
            val res = computeEnrichment(newStep, table, anno, params, nrRows)
            logger.info("1D-enrichment: addAnnotationsToResultTable.")
            val newHeaders = addAnnotationsToResultTable(newStep, table, newStep?.analysis?.result?.type, getAnnotationPath(anno), params)
            logger.info("1D-enrichment: addSelResults.")
            val newParams = addSelResults(params, nrRows)

            // add current step to usedBy in annotation
            annotationService?.addStepId(params.annotationId, newStep?.id)

            logger.info("1D-enrichment: all done.")

            newStep?.copy(
                results = gson.toJson(res),
                parameters = gson.toJson(newParams),
                commonResult = newStep?.commonResult?.copy(headers = newHeaders)
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun addAnnotationsToResultTable(step: AnalysisStep?, table: Table?, resType: String?, annotationPath: String?, params: OneDEnrichmentParams): List<Header>? {
        val newTable = AnnotationColumnAdd.addAnnotations(table, resType, annotationPath, params)
        // write table
        if(newTable != null) writeTableData.write(getOutputRoot() + step?.resultTablePath, newTable)
        // add new headers
        return newTable?.headers
    }

    private fun computeEnrichment(step: AnalysisStep?, table: Table?, anno: AnnotationInfo?, params: OneDEnrichmentParams, nrRows: Int): OneDEnrichment {
        val resType = step?.analysis?.result?.type

        // get categoryNames
        val categoryNames: List<String>? = params.categoryIds?.map{id -> anno?.headers?.find{it.id == id}?.name ?: ""}

        val enrichmentRows = comp?.computeEnrichment(table, resType, params, categoryNames, getAnnotationPath(anno))
        val enrichmentTable = saveResToTable(enrichmentRows, step?.resultPath)
        val selEnrichmentRows = getSelEnrichmentRows(enrichmentRows, nrRows)

        val colNames = (table?.headers?.filter { params.colIdxs?.contains(it.idx) ?: false})?.map{it.name ?: ""}

        val myAnno = EnrichmentAnnotationInfo(
            anno?.name,
            anno?.description,
            anno?.origFileName,
            anno?.nrEntries,
            anno?.creationString,
            categoryNames
        )

        return OneDEnrichment(enrichmentTable, selEnrichmentRows, myAnno, colNames)
    }

    private fun addSelResults(params: OneDEnrichmentParams, nrRows: Int): OneDEnrichmentParams {
        val selRes = (0 until nrRows).toList()
        return params.copy(selResults = selRes)
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
        return enrichmentRows?.sortedByDescending { it.median }?.mapIndexed { i, row -> row.copy(id = i) }?.take(nrRows)
    }

}