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
        stepDiv.add(titleDiv("$stepNr - Imputation", plotWidth = plotWidth))

        val colTable = Table(3)
        colTable.setWidth(plotWidth)
        val cellFifth = plotWidth/5

        // 1. parameters
        val paramsDiv = Div()
        val firstParam = listOf(getParagraph(imputationText[parsedParams.imputationType]?:"", bold = true))

        val additionalParams: List<Paragraph> =
            if (parsedParams.imputationType == "normal") listOf(getNormParams(parsedParams.normImputationParams))
            else if (parsedParams.imputationType == "value") listOf(getParagraph(parsedParams.replaceValue.toString()))
            else emptyList<Paragraph>()

        firstParam.plus(additionalParams).forEach{ paramsDiv.add(it) }
        colTable.addCell(getParamsCell(paramsDiv, 2*cellFifth))

        // 2. data
        val middleDiv = Div()
        val tableData: List<Pair<String, String>> = listOf(
            "Nr of imputed values:" to res.nrImputedValues.toString(),
            "Nr of protein groups with imputation:" to res.nrOfImputedGroups.toString()
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
    }

    private fun getNormParams(params: NormImputationParams?): Paragraph {
        val normParams = listOf(
            Pair("Width:", params?.width.toString()),
            Pair("Downshift:", params?.downshift.toString()),
            Pair("Seed:", params?.seed.toString())
        )
        val table = getTwoRowTable(normParams)
        val p = Paragraph()
        p.add(table)
        return p
    }

}
