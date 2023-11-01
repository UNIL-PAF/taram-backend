package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.log_transformation.LogTransformationParams
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.borders.*
import com.itextpdf.layout.element.*
import org.springframework.stereotype.Service


@Service
class FilterPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, Filter::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - Filter rows", plotWidth))

        val colTable = Table(3)
        colTable.setWidth(plotWidth)
        val cellFifth = plotWidth/5

        // 1. parameters
        val params = getParams(step)
        colTable.addCell(getParamsCell(params, 2*cellFifth))

        // 2. data
        val middleDiv = Div()
        val tableData = listOf(Pair("Protein groups removed:", res.nrRowsRemoved.toString()))
        middleDiv.add(getTwoRowTable(tableData))
        colTable.addCell(getDataCell(middleDiv, 2 * cellFifth))

        // 3. results
        val rightDiv = Div()
        rightDiv.add(getParagraph("${step.nrProteinGroups} protein groups"))
        rightDiv.add(getParagraph("Table $stepNr", bold = true, underline = true))
        colTable.addCell(getResultCell(rightDiv, cellFifth))

        stepDiv.add(colTable)
        return stepDiv
    }

    private fun getParams(step: AnalysisStep): Div {
        val paramDiv = Div()
        val parsedParams = gson.fromJson(step.parameters, FilterParams::class.java)
        val l = emptyList<Paragraph>()
        val l2 = if(parsedParams.removeOnlyIdentifiedBySite == true) l.plus(getParagraph("Remove only-identified-by-site")) else l
        val l3 = if(parsedParams.removeReverse == true) l2.plus(getParagraph("Remove reverse hits")) else l2
        val l4 = if(parsedParams.removePotentialContaminant == true) l3.plus(getParagraph("Remove potential contaminants")) else l3
        l4.plus(getFreeParams(parsedParams.colFilters)).forEach{paramDiv.add(it)}
        return paramDiv
    }

    private fun getFreeParams(colFilters: List<ColFilter>?): List<Paragraph> {
        return colFilters?.map{ flt ->
            val action = if(flt.removeSelected) "Remove" else "Only keep"
            val p = getParagraph("$action ")
            p.add(getText(" ${flt.colName} ", italic = true))
            p.add(getText("${flt.comparator.symbol} ${flt.compareToValue}"))
            p
        } ?: emptyList()
    }

}
