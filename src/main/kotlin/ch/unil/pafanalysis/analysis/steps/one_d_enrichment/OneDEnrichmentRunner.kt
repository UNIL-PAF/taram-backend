package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.summary_stat.SummaryStat
import ch.unil.pafanalysis.analysis.steps.volcano.VolcanoPlotParams
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.io.InputStream

@Service
class OneDEnrichmentRunner() : CommonStep(), CommonRunner {

    val version = "1.0"

    @Lazy
    @Autowired
    var asyncRunner: AsyncOneDEnrichmentRunner? = null

    @Autowired
    var enrichmentPdf: OneDEnrichmentPdf? = null

    val enrichmentTableReader = EnrichmentTableReader()

    override var type: AnalysisStepType? = AnalysisStepType.ONE_D_ENRICHMENT

    @Autowired
    private var env: Environment? = null

    private fun getOutputPath(): String? {
        return env?.getProperty("output.path")
    }

    fun getParameters(step: AnalysisStep?): OneDEnrichmentParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            OneDEnrichmentParams().javaClass
        ) else OneDEnrichmentParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, pageWidth: Float, stepNr: Int): Div? {
        return enrichmentPdf?.createPdf(step, pdf, pageWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, version, oldStepId, false, step, params)
        asyncRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        return "Parameter(s) changed:"
    }

    override fun switchSel(step: AnalysisStep?, idString: String): List<String>? {
        val origParams = gson.fromJson(step?.parameters, OneDEnrichmentParams().javaClass)
        val origList = origParams.selResults ?: emptyList()
        val id = idString.toInt()
        val remove: Boolean? = origList.contains(id)
        val newList = if(origList.contains(id)) origList.filter{it != id} else origList.plus(id)
        val newParams = origParams.copy(selResults = newList)
        val newRes = addOrRemoveNewRow(step, id, remove)
        analysisStepRepository?.saveAndFlush(step?.copy(parameters = gson.toJson(newParams), results = gson.toJson(newRes))!!)
        return newList.map{it.toString()}
    }

    private fun addOrRemoveNewRow(step: AnalysisStep?, id: Int, remove: Boolean?): OneDEnrichment?{
        val oldRes = gson.fromJson(step?.results, OneDEnrichment().javaClass)
        val enrichmentResFilePath = getOutputPath() + step?.resultPath + "/" + oldRes.enrichmentTable
        val fullTable = enrichmentTableReader.readTable(enrichmentResFilePath)
        val newRows = if(remove == true){
            oldRes.selResults?.filter{ r -> r.id != id}
        }else{
            val newRow = fullTable.rows?.find{ r -> r.id == id }!!
            oldRes.selResults?.plusElement(newRow)
        }?.sortedBy { it.pvalue }
        return oldRes?.copy(selResults = newRows)
    }

    fun getFullEnrichmentTable(step: AnalysisStep?): FullEnrichmentTable? {
        val result: OneDEnrichment = gson.fromJson(step?.results, OneDEnrichment().javaClass)
        val enrichmentResFilePath = getOutputPath() + step?.resultPath + "/" + result.enrichmentTable
        val fullTable = enrichmentTableReader.readTable(enrichmentResFilePath)
        return sortBySelected(fullTable, getParameters(step))
    }

    private fun sortBySelected(table: FullEnrichmentTable?, params: OneDEnrichmentParams): FullEnrichmentTable?{
        val rowWithSelInfo = table?.rows?.map{ r ->
            val isSel = params.selResults?.contains(r.id)
            Pair(r, isSel == false)
        }
        val sortedRow = rowWithSelInfo?.sortedWith(compareBy<Pair<EnrichmentRow, Boolean>>{it.second}.thenBy { it.first.pvalue })?.map{it.first}
        return table?.copy(rows = sortedRow)
    }

    override fun getResultByteArray(step: AnalysisStep?): ByteArray? {
        val result: OneDEnrichment = gson.fromJson(step?.results, OneDEnrichment().javaClass)
        val enrichmentResFilePath = getOutputPath() + step?.resultPath + "/" + result.enrichmentTable
        val inputStream: InputStream = FileInputStream(enrichmentResFilePath)
        return inputStream.readAllBytes()
    }
}