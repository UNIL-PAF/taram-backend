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
        val stepDiv = Div()
        val description = "Log transformation facilitates plotting of data and makes distributions more “normal”, allowing application of standard statistical tests."
        stepDiv.add(titleDiv("$stepNr. Log transformation", plotWidth = plotWidth, description = description, table = "Table $stepNr", nrProteins = step.nrProteinGroups, link = "$stepNr-${step.type}"))

        return stepDiv
    }

}
