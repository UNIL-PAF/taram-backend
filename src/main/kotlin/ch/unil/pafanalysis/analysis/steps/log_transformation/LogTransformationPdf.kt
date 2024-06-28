package ch.unil.pafanalysis.analysis.steps.log_transformation

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Table
import org.springframework.stereotype.Service


@Service
class LogTransformationPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, LogTransformation::class.java)
        val parsedParams = gson.fromJson(step.parameters, LogTransformationParams::class.java)

        val stepDiv = Div()
        val description = "Log transformation facilitates plotting of data and makes distributions more “normal”, allowing application of standard statistical tests."
        stepDiv.add(titleDiv("$stepNr. Log transformation", plotWidth = plotWidth, description = description, table = "Table-$stepNr", nrProteins = step.nrProteinGroups))

        val colTable = Table(2)
        colTable.setWidth(plotWidth)
        val colWidth = plotWidth/12

        // 1. parameters
        val paramsDiv = Div().setPaddingLeft(2f)
        paramsDiv.add(getParagraph(parsedParams.transformationType + " transformation", dense = true))
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
