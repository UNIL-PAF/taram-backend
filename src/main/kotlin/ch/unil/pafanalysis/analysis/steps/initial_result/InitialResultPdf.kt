package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.UnitValue
import org.springframework.stereotype.Service
import java.util.*


@Service
class InitialResultPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, document: Document?, pageSize: PageSize?, stepNr: Int): Document? {
        val initialResult = gson.fromJson(step.results, InitialResult::class.java)

        document?.add(horizontalLineDiv())
        val plotWidth = getPlotWidth(pageSize, document)
        document?.add(titleDiv("$stepNr - Initial result", initialResult.nrProteinGroups, step.tableNr, plotWidth))

        val fastaFileParagraph = Paragraph()
        initialResult.fastaFiles?.forEach{ fastaFileParagraph.add(Text(it + "\n")) }

        val tableData: List<Pair<String, Paragraph?>> = listOf(
            "Selected intensity column" to Paragraph(step?.columnInfo?.columnMapping?.intCol),
            "Fasta files" to fastaFileParagraph,
            "Software version" to Paragraph( initialResult.softwareVersion),
        )

        document?.add(addTwoRowTable(tableData))

        if(step.comments != null) document?.add(commentDiv(step.comments))
        return document
    }

}
