package ch.unil.pafanalysis.analysis.steps.rename_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.remove_imputed.RemoveImputedParams
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.springframework.stereotype.Service


@Service
class RenameColumnsPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val parsedParams = gson.fromJson(step.parameters, RenameColumnsParams::class.java)

        val stepDiv = Div()
        val description = "Add conditions to table headers."
        stepDiv.add(titleDiv("$stepNr. Rename headers", plotWidth = plotWidth, description = description, table = "Table $stepNr", nrProteins = step.nrProteinGroups, link = "$stepNr-${step.type}"))

        /*

        val colTable = Table(2)
        colTable.setWidth(plotWidth)
        val colWidth = plotWidth/12


        // 1. parameters
        val paramsDiv = Div().setPaddingLeft(2f)
        if(parsedParams.addConditionNames == true) paramsDiv.add(getParagraph("Add conditions to table headers.", dense = true))
        colTable.addCell(getParamsCell(paramsDiv, 8*colWidth))

        // 2. data
        val middleDiv = Div()
        colTable.addCell(getDataCell(middleDiv, 4 * colWidth))

        stepDiv.add(colTable)

         */
        return stepDiv
    }

}
