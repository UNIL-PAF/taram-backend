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

        val description = "Fold changes and p-values calculated with imputed data are kept."
        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr. Remove imputed", plotWidth, description = description, table = "Table $stepNr", nrProteins = step.nrProteinGroups, link = "$stepNr-${step.type}"))

        val colTable = Table(2)
        colTable.setWidth(plotWidth)
        val colWidth = plotWidth/12

        // 1. parameters
        val paramsDiv = Div().setPaddingLeft(2f)
        paramsDiv.add(getParagraph("Replace imputed values by [${parsedParams.replaceBy?.printName}].", dense = true))
        colTable.addCell(getParamsCell(paramsDiv, 7*colWidth))

        // 2. data
        val middleDiv = Div()
        val tableData: List<Pair<String, String>> = listOf(
            "Nr of replacements:" to res.nrValuesReplaced.toString(),
            "Nr of protein groups with replacements:" to res.nrProteinGroupsReplaced.toString()
        )
        middleDiv.add(getTwoRowTable(tableData))
        colTable.addCell(getDataCell(middleDiv, 5 * colWidth))

        stepDiv.add(colTable)
        return stepDiv
    }

}
