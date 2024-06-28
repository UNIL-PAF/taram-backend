package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ColumnMapping
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.borders.DoubleBorder
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import org.springframework.stereotype.Service


@Service
class InitialResultPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val initialResult = gson.fromJson(step.results, InitialResult::class.java)
        val groupsDefined = step.columnInfo?.columnMapping?.experimentDetails?.values?.find { it.group != null } != null

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr. Initial result", plotWidth, table = "Table-$stepNr", nrProteins = step.nrProteinGroups))

        // Groups definitions
        stepDiv.add(getParagraph("Groups and experiments:", bold = true))
        if(groupsDefined){
            val (groupHeaders, groupRows) = getGroups(step.columnInfo?.columnMapping)
            if(groupHeaders != null && groupRows != null){
                val tables =  createTables(groupHeaders, groupRows, plotWidth)
                tables.forEach{ table ->
                    stepDiv.add(table)
                    stepDiv.add(Paragraph(""))
                }
            }
        }else{
            stepDiv.add(Paragraph("No groups are defined"))
        }

        // 1. parameters
        val leftDiv = Div()
        val dataTable = getDataTable(initialResult, step?.columnInfo?.columnMapping?.intCol ?: "")
        leftDiv.add(getTwoRowTableWithList(dataTable).setWidth(plotWidth))
        stepDiv.add(leftDiv)

        return stepDiv
    }

    private fun createTables(groupHeaders: List<String>, groupRows: List<List<String?>>, plotWidth: Float): List<Table> {
        val nrEntries = groupHeaders.size

        val maxChar = 110

        var charsUsed = 0
        var start = 0
        var end = 0
        var tables = emptyList<Table>()
        var i = 0

        while(i < nrEntries){
            val longestChar = listOf(groupHeaders[i].length, groupRows[0][i]?.length ?: 0, 8).maxOrNull() ?: 8
            charsUsed += longestChar
            if(charsUsed > maxChar){
                tables += createTable(groupHeaders, groupRows, start, end, plotWidth)
                start = i
                charsUsed = longestChar
            }
            i++
            end = i
        }

        // add last table if necessary
        if(end > start){
            tables += createTable(groupHeaders, groupRows, start, end, plotWidth)
        }
        return tables
    }

    private fun createTable(
        groupHeaders: List<String>,
        groupRows: List<List<String?>>,
        start: Int,
        end: Int,
        plotWidth: Float
    ): Table {
        val table = Table(end-start).setMarginBottom(10f).setKeepTogether(true) //.setWidth(plotWidth)
        groupHeaders.subList(start, end).forEach{
            val cell = Cell().add(getParagraph(it, bold = true))
                .setBorderTop(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
                .setBorderBottom(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
                .setBorderLeft(DoubleBorder(ColorConstants.LIGHT_GRAY, 2f))
                .setBorderRight(DoubleBorder(ColorConstants.LIGHT_GRAY, 2f))
            table.addCell(cell)
        }
        groupRows.forEach { it.subList(start, end).forEach{ v ->
            val cell = Cell().add(getParagraph(v ?: ""))
                .setBorderTop(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
                .setBorderBottom(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
                .setBorderLeft(DoubleBorder(ColorConstants.LIGHT_GRAY, 2f))
                .setBorderRight(DoubleBorder(ColorConstants.LIGHT_GRAY, 2f))
            table.addCell(cell)
        }}
        return table
    }

    private fun getGroups(colMapping: ColumnMapping?): Pair<List<String>?, List<List<String?>>?> {
        val listByGroups: List<Pair<String, List<String>>>? =  colMapping?.experimentNames?.fold(emptyList()){ acc, exp ->
            val details = colMapping?.experimentDetails?.get(exp)
            val newAcc = if(details?.group != null && details.group.isNotEmpty()){
                val currGroup = acc.find{it.first == details.group}
                val newGroup: Pair<String, List<String>> =
                    currGroup?.copy(second = currGroup.second.plus(exp)) ?: Pair(details.group, listOf(exp))
                val fltList: List<Pair<String, List<String>>>? = acc.filter{it.first != details?.group}
                fltList?.plus(newGroup) ?: listOf(newGroup)
            }else acc
            newAcc
        }

        val nrRows = listByGroups?.map{it.second.size }?.maxOrNull()?.minus(1)
        return if(nrRows != null){
            val headers = listByGroups.map{it.first}
            val rows = (0..nrRows).map { i -> listByGroups.map{ it.second.getOrNull(i)} }
            Pair(headers, rows)
        } else Pair(null, null)
    }

    private fun getDataTable(initialResult: InitialResult, intCol: String): List<Pair<String, List<String>>>{
        return if(initialResult?.spectronautSetup != null){
            listOf(
                Pair("Default intensity column:", listOf(intCol)),
                Pair("Software version: ", listOf(initialResult?.softwareVersion ?: "")),
                Pair("Analysis date: ", listOf(initialResult.spectronautSetup.analysisDate ?: "")),
                Pair("Fasta files:", initialResult.fastaFiles ?: emptyList()),
                Pair("Libraries:", initialResult.spectronautSetup.libraries?.map{it.name ?: ""} ?: emptyList())
            )
        }else{
            listOf(
                Pair("Default intensity column:", listOf(intCol)),
                Pair("Software version: ", listOf(initialResult?.softwareVersion ?: "")),
                Pair("Fasta files:", initialResult.fastaFiles ?: emptyList()),
                Pair("Match between runs:", listOf(if(initialResult.maxQuantParameters?.matchBetweenRuns == true) "TRUE" else if(initialResult.maxQuantParameters?.matchBetweenRuns == false) "FALSE" else ""  ))
            )
        }
    }

}
