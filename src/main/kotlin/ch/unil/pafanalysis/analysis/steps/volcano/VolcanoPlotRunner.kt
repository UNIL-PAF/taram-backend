package ch.unil.pafanalysis.analysis.steps.volcano

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.service.AsyncAnalysisStepService
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class VolcanoPlotRunner() : CommonStep(), CommonRunner {

    val version = "1.0"

    override var type: AnalysisStepType? = AnalysisStepType.VOLCANO_PLOT

    @Autowired
    var asyncVolcanoPlotRunner: AsyncVolcanoPlotRunner? = null

    @Autowired
    private var asyncAnaysisStepService: AsyncAnalysisStepService? = null


    @Autowired
    private var volcanoPdf: VolcanoPdf? = null

    fun getParameters(step: AnalysisStep?): VolcanoPlotParams {
        return if(step?.parameters != null) gson.fromJson(step?.parameters, VolcanoPlotParams().javaClass) else VolcanoPlotParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div? {
        return volcanoPdf?.createPdf(step, pdf, plotWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val myParams = params ?: step?.parameters
        val volcanoParams = gson.fromJson(myParams, VolcanoPlotParams().javaClass)

        val lastStep: AnalysisStep? = if(volcanoParams.plotAllComps == true){
            val oldStep: AnalysisStep? = analysisStepRepository?.findById(oldStepId)
            val availableComps = oldStep?.commonResult?.headers?.mapNotNull { it.experiment?.comp }?.distinct()

            val newSteps = availableComps?.fold(Pair(emptyList<AnalysisStep?>(), oldStepId)) { acc, comp ->
                val newParams = volcanoParams?.copy(comparison = ComparisonParams(group1 = comp.group1, group2 = comp.group2), plotAllComps = false)
                val newStep = runCommonStep(type!!, version, acc.second, false, step, gson.toJson(newParams))
                Pair(acc.first.plus(newStep), newStep!!.id!!)
            }?.first

            if((newSteps?.size ?: 0) > 1){
                asyncAnaysisStepService?.setAllStepsStatus(newSteps?.get(1), AnalysisStepStatus.IDLE)
            }

            asyncVolcanoPlotRunner?.runAsync(newSteps?.first())
            newSteps?.last()
        }else{
            val newStep = runCommonStep(type!!, version, oldStepId, false, step, params)
            asyncVolcanoPlotRunner?.runAsync(newStep)
            newStep
        }
        return lastStep!!
    }

    override fun updatePlotOptions(step: AnalysisStep, echartsPlot: EchartsPlot): String {
        val newResults = gson.fromJson(step.results, VolcanoPlot().javaClass).copy(plot = echartsPlot)
        val newStep = step.copy(results = gson.toJson(newResults))
        analysisStepRepository?.saveAndFlush(newStep)
        return echartsPlot.echartsHash.toString()
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        // there might be differences in selected proteins, which we ignore
        val message = (if (params.pValThresh != origParams?.pValThresh) " [P-value threshold: ${params.pValThresh}]" else "")
            .plus(if (params.fcThresh != origParams?.fcThresh) " [Significance threshold: ${params.fcThresh}]" else "")
            .plus(if (params.useAdjustedPVal != origParams?.useAdjustedPVal) " [Use adjusted p-value: ${params.useAdjustedPVal}]" else "")
            .plus(if (params.log10PVal != origParams?.log10PVal) " [Use log10 p-value: ${params.log10PVal}]" else "")

        return if(message != "") "Parameter(s) changed:".plus(message) else null
    }

    override fun switchSel(step: AnalysisStep?, proteinAc: String): List<String>? {
        val origParams = gson.fromJson(step?.parameters, VolcanoPlotParams().javaClass)
        val origList = origParams.selProteins ?: emptyList()
        val newList = if(origList.contains(proteinAc)) origList.filter{it != proteinAc} else origList.plus(proteinAc)
        val newParams = origParams.copy(selProteins = newList)
        analysisStepRepository?.saveAndFlush(step?.copy(parameters = gson.toJson(newParams))!!)
        return newList
    }

    override fun getResult(step: AnalysisStep?): String? {
        val res = gson.fromJson(step?.results, VolcanoPlot().javaClass)
        val withoutPlot = res.copy(plot = null)
        return gson.toJson(withoutPlot)
    }

}