package ch.unil.pafanalysis.analysis.steps.add_column

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.filter.FilterParams
import ch.unil.pafanalysis.analysis.steps.remove_columns.RemoveColumns
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.springframework.stereotype.Service


@Service
class AddColumnPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val params = gson.fromJson(step.parameters, AddColumnParams::class.java)
        val res = gson.fromJson(step.results, AddColumn::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr. Add column", plotWidth = plotWidth, table = "Table $stepNr", nrProteins = step.nrProteinGroups, link = "$stepNr-${step.type}"))

        val colTable = Table(2)
        colTable.setWidth(plotWidth)
        val colWidth = plotWidth/12

        // 1. parameters
        val paramsDiv = getParamsDiv(params, res)
        colTable.addCell(getParamsCell(paramsDiv, 8*colWidth))

        // 2. data
        val tableData: List<Pair<String, String>> = listOf(
            Pair("Added new column:", params.newColName!!)
        )
        val middleDiv = Div()
        middleDiv.add(getTwoRowTable(tableData))
        colTable.addCell(getDataCell(middleDiv, 4 * colWidth))

        stepDiv.add(colTable)
        return stepDiv
    }

    private fun getParamsDiv(params: AddColumnParams, res: AddColumn): Div {
        val paramDiv = Div()

        val paramPara = if(params.type == SelColType.CHAR){
            val p = getParagraph("Mark each row with ", dense = true)
            p.add(getText("+ ", italic = true))
            p.add(getText("where "))
            p.add(getText(params.charColParams?.compSel?.value!! + " ", italic = true))

            val entryOrEntries = if(params.charColParams?.compSel == CompSel.ANY) "entry" else "entries"
            val charComp = if(params.charColParams.compSel == CompSel.ANY){
                if(params.charColParams.compOp == CompOp.MATCHES) "matches" else "does not match"
            } else {
                if(params.charColParams.compOp == CompOp.MATCHES) "match" else "do not match"
            }

            p.add(getText("$entryOrEntries from "))
            p.add(getText("[${res.selColNames?.joinToString(separator = ", ")}] ", italic = true))
            p.add(getText("$charComp "))
            p.add(getText("${params.charColParams.compVal}.", italic = true))
            p
        }else{
            val p = getParagraph("Compute the ", dense = true)
            p.add(getText("${params.numColParams?.mathOp?.value} ", italic = true))
            p.add(getText("of columns "))
            p.add(getText("[${res.selColNames?.joinToString(separator = ", ")}].", italic = true))
            p
        }
        return paramDiv.add(paramPara)
    }

}
