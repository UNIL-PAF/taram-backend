package ch.unil.pafanalysis.analysis.steps

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import java.io.File

interface CommonRunner {
    fun run(oldStepId: Int, step: AnalysisStep? = null, params: String? = null): AnalysisStep

    fun updatePlotOptions(step: AnalysisStep, echartsPlot: EchartsPlot): String {
        throw Exception("'updatePlotOptions' is not implemented for this Runner [${step?.type}].")
    }

    fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div?

    fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String?

    fun getResultByteArray(step: AnalysisStep?): ByteArray? {
        throw Exception("'getResultByteArray' is not implemented for [${step?.type}].")
    }

    fun getOtherTable(step: AnalysisStep?, tableDir: String, idx: Int): File? {
        throw Exception("'getOtherTable' is not implemented for [${step?.type}].")
    }

    fun getOtherTableName(idx: Int): String? {
        throw Exception("'getOtherTableName' is not implemented for this type.")
    }

    fun switchSel(step: AnalysisStep?, selId: String): List<String>? {
        throw StepException("Cannot select items for [$step?.type].")
    }

    fun getResult(step: AnalysisStep?): String? {
        throw StepException("getResult is not implemented for [$step?.type].")
    }

}