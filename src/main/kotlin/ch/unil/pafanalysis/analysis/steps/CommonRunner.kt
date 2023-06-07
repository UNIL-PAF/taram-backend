package ch.unil.pafanalysis.analysis.steps

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Div

interface CommonRunner {
    fun run(oldStepId: Int, step: AnalysisStep? = null, params: String? = null): AnalysisStep

    fun updatePlotOptions(step: AnalysisStep, echartsPlot: EchartsPlot): String {
        throw Exception("'updatePlotOptions' is not implemented for this Runner [${step?.type}].")
    }

    fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div?

    fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String?

    fun getResultByteArray(step: AnalysisStep?): ByteArray? {
        throw Exception("'getResultByteArray' is not implemented for this Runner [${step?.type}].")
    }
}