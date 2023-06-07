package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.stereotype.Service


@Service
class InitialResultPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val initialResult = gson.fromJson(step.results, InitialResult::class.java)

        val div = Div()
        div.add(horizontalLineDiv(plotWidth))
        div.add(titleDiv("$stepNr - Initial result", initialResult.nrProteinGroups, step.tableNr, plotWidth))

        val fastaFileParagraph = Paragraph()
        initialResult.fastaFiles?.forEach{ fastaFileParagraph.add(Text(it + "\n")) }

        val tableData: List<Pair<String, Paragraph?>> = listOf(
            "Selected intensity column" to Paragraph(step?.columnInfo?.columnMapping?.intCol),
            "Fasta files" to fastaFileParagraph,
            "Software version" to Paragraph( initialResult.softwareVersion),
        )

        div.add(addTwoRowTable(tableData))

        if(step.comments != null) div.add(commentDiv(step.comments))
        return div
    }

}
