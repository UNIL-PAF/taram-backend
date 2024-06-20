package ch.unil.pafanalysis.analysis.steps.imputation

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
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

    private fun getImputationText(params: ImputationParams, step: AnalysisStep): String {
        val selColTxt = getSelColTxt(params, step)
        return when (params.imputationType) {
            "normal" -> "Replace missing values in column(s) [$selColTxt] by random numbers drawn from a normal distribution:"
            "nan" -> "Replace missing values in column(s) [$selColTxt] by NaN."
            "value" -> "Replace missing values in column(s) [$selColTxt] by ${params.replaceValue}."
            else -> throw Exception("Imputation type [${params.imputationType}] is not implemented.")
        }
    }

    private fun getSelColTxt(params: ImputationParams, step: AnalysisStep): String {
        return if (params.intCol == null && params.selColIdxs == null) {
            return step.columnInfo?.columnMapping?.intCol + "..."
        } else if (params.intCol == null && !params.selColIdxs.isNullOrEmpty()) {
            val selColNames =
                step.commonResult?.headers?.filter { h -> params.selColIdxs.contains(h.idx) }?.map { it.name }
            return selColNames?.joinToString(", ") ?: ""
        } else {
            return params.intCol + "..."
        }
    }

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, Imputation::class.java)
        val parsedParams = gson.fromJson(step.parameters, ImputationParams::class.java)

        val stepDiv = Div()
        val description = "Imputation allows calculating fold changes (FC) and statistical tests for proteins with missing values. NaN is replaced by low-shifted random values, based on the assumption that missing values occur when signals are below detection limits."
        stepDiv.add(titleDiv("$stepNr. Imputation", plotWidth = plotWidth, description))

        val colTable = Table(3)
        colTable.setWidth(plotWidth)
        val cellFifth = plotWidth / 5

        // 1. parameters
        val paramsDiv = Div()
        val firstParam = listOf(getParagraph(getImputationText(parsedParams, step), bold = false))

        val additionalParams: List<Paragraph> =
            if (parsedParams.imputationType == "normal") listOf(getNormParams(parsedParams.normImputationParams))
            else emptyList()

        firstParam.plus(additionalParams).forEach { paramsDiv.add(it) }
        colTable.addCell(getParamsCell(paramsDiv, 2 * cellFifth))

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
        rightDiv.add(getTableParagraph(stepNr.toString()))
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
        val table = getTwoRowTable(normParams, noBold = true)
        val p = Paragraph()
        p.add(table)
        return p
    }

}
