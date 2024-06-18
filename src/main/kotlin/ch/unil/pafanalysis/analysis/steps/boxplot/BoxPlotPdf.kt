package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.common.EchartsServer
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class BoxPlotPdf() : PdfCommon() {

    @Autowired
    private var echartsServer: EchartsServer? = null

    fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div {
        val div = Div()
        val description = "Overview of samples. Y-axis is usually in log2 scale."
        div.add(titleDiv("$stepNr. Boxplot", plotWidth, description))

        div.add(Paragraph(" "))
        val plot = echartsServer?.makeEchartsPlot(step, pdf, plotWidth)
        div.add(plot)
        return div
    }

}
