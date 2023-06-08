package ch.unil.pafanalysis.analysis.steps.normalization

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
class NormalizationPdf() : PdfCommon() {

    val normType = mapOf(
        "median" to "Median",
        "mean" to "Mean",
        "none" to "None"
    )

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, Normalization::class.java)
        val parsedParams = gson.fromJson(step.parameters, NormalizationParams::class.java)

        val stepDiv = Div()

        stepDiv.add(horizontalLineDiv(plotWidth))
        stepDiv.add(titleDiv("$stepNr - Normalization", step.nrProteinGroups, step.tableNr, plotWidth = plotWidth))

        val colTable = Table(2)
        colTable.setWidth(plotWidth)

        val tableData: List<Pair<String, Paragraph?>> = listOf(
            "Min" to Paragraph(String.format("%.2f", res.min)),
            "Max" to Paragraph(String.format("%.2f", res.max)),
            "Mean" to Paragraph(String.format("%.2f", res.mean)),
            "Median" to Paragraph(String.format("%.2f", res.median)),
            "Sum" to Paragraph(String.format("%.2f", res.sum)),
            "Nr of valid" to Paragraph(res.nrValid?.toString()),
            "Nr of NaN" to Paragraph(res.nrNaN?.toString())
        )

        val leftCell = Cell().add(addTwoRowTable(tableData))
        leftCell.setWidth(plotWidth/2)
        leftCell.setBorder(Border.NO_BORDER)
        colTable.addCell(leftCell)

        val params = parametersDiv(listOf(Paragraph(normType[parsedParams.normalizationType] + " normalization")))
        val rightCell = Cell().add(params)
        colTable.addCell(rightCell)

        stepDiv.add(colTable)

        if(step.comments != null) stepDiv.add(commentDiv(step.comments))
        return stepDiv
    }

}
