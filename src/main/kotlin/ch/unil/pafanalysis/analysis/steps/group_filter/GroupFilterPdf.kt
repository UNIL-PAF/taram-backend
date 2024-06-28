package ch.unil.pafanalysis.analysis.steps.group_filter

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.log_transformation.LogTransformationParams
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.borders.*
import com.itextpdf.layout.element.*
import org.springframework.stereotype.Service


@Service
class GroupFilterPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, GroupFilter::class.java)

        val stepDiv = Div()
        val description = "Retain only proteins quantified in a minimum number of samples."
        stepDiv.add(titleDiv("$stepNr. Filter on valid", plotWidth, description = description, table = "Table-$stepNr", nrProteins = step.nrProteinGroups))

        val colTable = Table(2)
        colTable.setWidth(plotWidth)
        val colWidth = plotWidth/12

        // 1. parameters
        val paramsDiv = getParams(step)
        colTable.addCell(getParamsCell(paramsDiv, 8*colWidth))

        // 2. data
        val middleDiv = Div()
        val tableData = listOf(Pair("Protein groups removed:", res.nrRowsRemoved.toString()))
        middleDiv.add(getTwoRowTable(tableData))
        colTable.addCell(getDataCell(middleDiv, 4 * colWidth))

        stepDiv.add(colTable)
        return stepDiv
    }

    private fun getParams(step: AnalysisStep): Div {
        val parsedParams = gson.fromJson(step.parameters, GroupFilterParams::class.java)

        val groupTxt = mapOf<String, String>("one_group" to "one group", "all_groups" to "all groups", "total" to "total")

        val myIntField = if(parsedParams.field != null) parsedParams.field else step.columnInfo?.columnMapping?.intCol

        val p = getParagraph("Only keep rows where ", dense = true)
        p.add(getText(myIntField ?: "", italic = true))
        p.add(getText(" has at least ${parsedParams.minNrValid} valid value(s) in ${groupTxt[parsedParams.filterInGroup]}."))

        val d = Div()
        d.add(p)
        return d
    }

}
