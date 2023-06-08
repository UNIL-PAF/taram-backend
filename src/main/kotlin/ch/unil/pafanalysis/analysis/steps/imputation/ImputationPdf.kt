package ch.unil.pafanalysis.analysis.steps.imputation

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
class ImputationPdf() : PdfCommon() {

    val imputationText = mapOf(
        "normal" to "Replace missing values from normal distribution:",
        "nan" to "Replace missing values by NaN.",
        "value" to "Replace missing values by "
    )

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, Imputation::class.java)
        val parsedParams = gson.fromJson(step.parameters, ImputationParams::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - Imputation", step.nrProteinGroups, step.tableNr, plotWidth = plotWidth))

        val colTable = Table(2)
        colTable.setWidth(plotWidth)

        val tableData: List<Pair<String, Paragraph?>> = listOf(
            "Nr of imputed values" to Paragraph(res.nrImputedValues.toString()),
        )

        val leftCell = Cell().add(addTwoRowTable(tableData))
        leftCell.setWidth(plotWidth / 2)
        leftCell.setBorder(Border.NO_BORDER)
        colTable.addCell(leftCell)

        val firstParam = listOf(Paragraph(imputationText[parsedParams.imputationType]))

        val additionalParams: List<Paragraph> =
            if (parsedParams.imputationType == "normal") getNormParams(parsedParams.normImputationParams)
            else if (parsedParams.imputationType == "value") listOf(Paragraph(parsedParams.replaceValue.toString()))
            else emptyList<Paragraph>()

        val allParams: List<Paragraph> = firstParam.plus(additionalParams)

        val rightCell = Cell().add(parametersDiv(allParams))
        colTable.addCell(rightCell)

        stepDiv.add(colTable)
        return stepDiv
    }

    private fun getNormParams(params: NormImputationParams?): List<Paragraph> {
        return listOf<Paragraph>(
            Paragraph("Width: ${params?.width.toString()}"),
            Paragraph("Downshift: ${params?.downshift.toString()}"),
            Paragraph("Seed: ${params?.seed.toString()}")
        )
    }

}
