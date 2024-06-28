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

    fun normText(params: NormalizationParams): String {
        val myType = normType[params.normalizationType]
        return if(params.normalizationCalculation == NormalizationCalculation.DIVISION.value){
            "Divide by $myType"
        }else{
            "Subtract $myType"
        }
    }

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, Normalization::class.java)
        val parsedParams = gson.fromJson(step.parameters, NormalizationParams::class.java)

        val stepDiv = Div()
        val description1 = "Assumption: the majority of the observed proteins remain unchanged."
        val description2 = if(parsedParams?.normalizationType == NormalizationType.MEDIAN.value) "Median subtraction is the most conservative normalization, used to compensate for global differences in sample amounts. " else ""
        stepDiv.add(titleDiv("$stepNr. Normalization", plotWidth = plotWidth, description = description2.plus(description1), table = "Table-$stepNr", nrProteins = step.nrProteinGroups))

        val colTable = Table(2)
        colTable.setWidth(plotWidth)
        val colWidth = plotWidth/12

        // 1. parameters
        val paramsDiv = Div().setPaddingLeft(2f)
        paramsDiv.add(getParagraph(normText(parsedParams), dense = true))
        colTable.addCell(getParamsCell(paramsDiv, 10*colWidth))

        // 2. data
        val middleDiv = Div()
        val tableData: List<Pair<String, String>> = listOf(
            "Min:" to String.format("%.2f", res.min),
            "Max:" to String.format("%.2f", res.max),
        )
        middleDiv.add(getTwoRowTable(tableData))
        colTable.addCell(getDataCell(middleDiv, 2 * colWidth))

        stepDiv.add(colTable)
        return stepDiv
    }

}
