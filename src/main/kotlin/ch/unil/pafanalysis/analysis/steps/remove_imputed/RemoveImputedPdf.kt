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
        stepDiv.add(titleDiv("$stepNr - Remove imputed", plotWidth))

        val description = "FC and p-values calculated with imputed data are kept."
        stepDiv.add(descriptionDiv(description))

        val colTable = Table(3)
        colTable.setWidth(plotWidth)
        val cellFifth = plotWidth/5

        // 1. parameters
        val paramsDiv = Div()
        paramsDiv.add(getParagraph("Replace imputed values by [${parsedParams.replaceBy?.printName}]."))
        colTable.addCell(getParamsCell(paramsDiv, 2*cellFifth))

        // 2. data
        val middleDiv = Div()
        val tableData: List<Pair<String, String>> = listOf(
            "Nr of replacements:" to res.nrValuesReplaced.toString(),
            "Nr of protein groups with replacements:" to res.nrProteinGroupsReplaced.toString()
        )
        middleDiv.add(getTwoRowTable(tableData))
        colTable.addCell(getDataCell(middleDiv, 2 * cellFifth))

        // 3. results
        val rightDiv = Div()
        rightDiv.add(getParagraph("${step.nrProteinGroups} protein groups"))
        rightDiv.add(getParagraph("Table-$stepNr", bold = true, underline = true))
        colTable.addCell(getResultCell(rightDiv, cellFifth))

        stepDiv.add(colTable)
        return stepDiv
    }

}
