package ch.unil.pafanalysis.analysis.steps.remove_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.springframework.stereotype.Service


@Service
class RemoveColumnsPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, RemoveColumns::class.java)

        val stepDiv = Div()

        stepDiv.add(horizontalLineDiv(plotWidth))
        stepDiv.add(titleDiv("$stepNr - Remove columns", step.nrProteinGroups, step.tableNr, plotWidth = plotWidth))

        val tableData: List<Pair<String, Paragraph?>> = listOf(
            "Nr of columns" to Paragraph(res.nrOfColumns.toString()),
            "Nr of columns removed" to Paragraph(res.nrOfColumnsRemoved.toString())
        )

        stepDiv.add(addTwoRowTable(tableData))

        if(step.comments != null) stepDiv.add(commentDiv(step.comments))
        return stepDiv
    }

}
