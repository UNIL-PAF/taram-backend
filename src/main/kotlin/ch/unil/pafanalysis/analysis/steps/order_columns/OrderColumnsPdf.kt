package ch.unil.pafanalysis.analysis.steps.order_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import org.springframework.stereotype.Service


@Service
class OrderColumnsPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, OrderColumns::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - Order columns", plotWidth = plotWidth))

        val tableData: List<Pair<String, Paragraph?>> = listOf(
            "Nr of columns moved" to Paragraph(res.nrColumnsMoved.toString())
        )

        stepDiv.add(addTwoRowTable(tableData))
        return stepDiv
    }

}
