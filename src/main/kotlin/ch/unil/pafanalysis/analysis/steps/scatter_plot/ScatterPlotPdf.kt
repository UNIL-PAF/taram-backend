package ch.unil.pafanalysis.analysis.steps.scatter_plot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.common.EchartsServer
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ScatterPlotPdf() : PdfCommon() {

    @Autowired
    private var echartsServer: EchartsServer? = null

    fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div {
        val div = Div()
        div.add(titleDiv("$stepNr. Scatter plot", plotWidth, link = "$stepNr-${step.type}"))
        div.add(Paragraph(" "))
        val plot = echartsServer?.makeEchartsPlot(step, pdf, plotWidth)
        div.add(plot)

        val scatterParams = gson.fromJson(step?.parameters, ScatterPlotParams().javaClass)
        val scatterPlot = gson.fromJson(step?.results, ScatterPlot().javaClass)
        val hasMultiGene = scatterPlot.data?.any{ a ->
            scatterParams.selProteins?.contains(a.ac) ?: false && a.n?.contains("*") ?: false
        }
        if(hasMultiGene == true) div.add(getParagraph("* only the first of multiple gene names is displayed."))

        return div
    }

}
