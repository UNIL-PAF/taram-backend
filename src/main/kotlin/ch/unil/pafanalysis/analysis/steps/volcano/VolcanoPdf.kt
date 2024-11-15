package ch.unil.pafanalysis.analysis.steps.volcano

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.steps.scatter_plot.ScatterPlot
import ch.unil.pafanalysis.analysis.steps.scatter_plot.ScatterPlotParams
import ch.unil.pafanalysis.common.EchartsServer
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class VolcanoPdf() : PdfCommon() {

    @Autowired
    private var echartsServer: EchartsServer? = null

    @Autowired
    private var analysisStepRepo: AnalysisStepRepository? = null

    fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div {
        val div = Div()

        // check if there is already a volcano plot shown before
        val volcanoBefore = findVolcanoBefore(step?.beforeId)
        val description = if(volcanoBefore == false) "y-axis: can be either adjusted or unadjusted p-values (-log10)." else null

        div.add(titleDiv("$stepNr. Volcano plot", plotWidth, description = description, link = "$stepNr-${step.type}"))

        div.add(Paragraph(" "))
        val plot = echartsServer?.makeEchartsPlot(step, pdf, plotWidth)
        div.add(plot)

        val volcanoParams = gson.fromJson(step?.parameters, VolcanoPlotParams().javaClass)
        val volcanoPlot = gson.fromJson(step?.results, VolcanoPlot().javaClass)
        val hasMultiGene = volcanoPlot.data?.any{ a ->
            volcanoParams.selProteins?.contains(a.prot) ?: false && a.multiGenes ?: false
        }
        if(hasMultiGene == true) div.add(getParagraph("* only the first of multiple gene names is displayed."))

        return div
    }

    private fun findVolcanoBefore(stepId: Int?): Boolean? {
        if(stepId == null) return false
        val step = analysisStepRepo?.findById(stepId)
        return if(step?.type == AnalysisStepType.VOLCANO_PLOT.value) true else findVolcanoBefore(step?.beforeId)
    }

}
