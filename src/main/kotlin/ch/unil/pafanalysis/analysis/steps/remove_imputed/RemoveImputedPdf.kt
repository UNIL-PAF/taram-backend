package ch.unil.pafanalysis.analysis.steps.remove_imputed

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.log_transformation.LogTransformationParams
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.borders.*
import com.itextpdf.layout.element.*
import org.springframework.stereotype.Service


@Service
class RemoveImputedPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, RemoveImputed::class.java)
        val parsedParams = gson.fromJson(step.parameters, RemoveImputedParams::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - Remove imputed", step.nrProteinGroups, step.tableNr, plotWidth))

        val colTable = Table(2)
        val tableMargin = 5f
        colTable.setWidth(plotWidth - tableMargin)

        val tableData: List<Pair<String, Paragraph?>> = listOf(
            "Nr of replacements" to Paragraph(res.nrValuesReplaced.toString()),
            "Nr of protein groups with replacements" to Paragraph(res.nrProteinGroupsReplaced.toString())
        )

        val leftCell = Cell().add(addTwoRowTable(tableData))
        leftCell.setWidth(plotWidth/2)
        leftCell.setBorder(Border.NO_BORDER)
        colTable.addCell(leftCell)

        val params = parametersDiv(listOf(Paragraph("Replace imputed values by ${parsedParams.replaceBy?.printName}")))
        val rightCell = Cell().add(params)
        rightCell.setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
        colTable.addCell(rightCell)
        colTable.setMarginRight(tableMargin)
        colTable.setMarginBottom(tableMargin)

        stepDiv.add(colTable)
        return stepDiv
    }

}
