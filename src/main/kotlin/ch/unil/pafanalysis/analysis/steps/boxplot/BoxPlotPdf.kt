package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.common.EchartsServer
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class BoxPlotPdf() : PdfCommon() {

    @Autowired
    private var echartsServer: EchartsServer? = null

    fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div {
        val div = Div()
        div.add(horizontalLineDiv(plotWidth))
        div.add(titleDiv("$stepNr - Boxplot", step?.nrProteinGroups, step.tableNr, plotWidth))
        div.add(Paragraph(" "))
        val plot = echartsServer?.makeEchartsPlot(step, pdf, plotWidth)
        div.add(plot)
        if (step.comments !== null) div.add(Paragraph().add(Text(step.comments)))
        return div
    }

}
