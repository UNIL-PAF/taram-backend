package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import org.springframework.stereotype.Service


@Service
class FilterPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, Filter::class.java)

        val stepDiv = Div()
        stepDiv.add(horizontalLineDiv(plotWidth))
        stepDiv.add(titleDiv("$stepNr - Filter rows", step.nrProteinGroups, step.tableNr, plotWidth))

        val tableData: List<Pair<String, Paragraph?>> = listOf(
            "Rows removed" to Paragraph(res.nrRowsRemoved.toString())
        )

        stepDiv.add(addTwoRowTable(tableData))
        if(step.comments != null) stepDiv.add(commentDiv(step.comments))
        return stepDiv
    }

}
