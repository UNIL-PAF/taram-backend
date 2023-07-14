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
        stepDiv.add(titleDiv("$stepNr - Normalization", plotWidth = plotWidth))

        val colTable = Table(3)
        colTable.setWidth(plotWidth)
        val cellFifth = plotWidth/5

        // 1. parameters
        val paramsDiv = Div()
        paramsDiv.add(getParagraph(normType[parsedParams.normalizationType] + " normalization"))
        colTable.addCell(getParamsCell(paramsDiv, 2*cellFifth))

        // 2. data
        val middleDiv = Div()
        val tableData: List<Pair<String, String>> = listOf(
            "Min" to String.format("%.2f", res.min),
            "Max" to String.format("%.2f", res.max),
        )
        middleDiv.add(getTwoRowTable(tableData))
        colTable.addCell(getDataCell(middleDiv, 2 * cellFifth))

        // 3. results
        val rightDiv = Div()
        rightDiv.add(getParagraph("${step.nrProteinGroups} protein groups"))
        rightDiv.add(getParagraph("Table ${step.tableNr}"))
        colTable.addCell(getResultCell(rightDiv, cellFifth))

        stepDiv.add(colTable)
        return stepDiv
        /*
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

         */
    }

}
