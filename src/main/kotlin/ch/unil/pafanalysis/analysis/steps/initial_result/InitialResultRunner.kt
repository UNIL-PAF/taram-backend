package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.results.model.Result
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.time.LocalDateTime


@Service
class InitialResultRunner() : CommonStep(), CommonRunner {

    @Lazy
    @Autowired
    var asyncInitialResultRunner: AsyncInitialResultRunner? = null

    @Autowired
    private var initialResultPdf: InitialResultPdf? = null

    override var type: AnalysisStepType? = AnalysisStepType.INITIAL_RESULT

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, pageWidth: Float, stepNr: Int): Div? {
        return initialResultPdf?.createPdf(step, pdf, pageWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        throw Exception("InitialResultRunner does not implement ordinary run function.")
    }

    fun prepareRun(analysisId: Int?, result: Result?): AnalysisStep? {
        val analysis =
            analysisRepository?.findById(analysisId ?: throw StepException("No valid analysisId was provided."))
        return createEmptyInitialResult(analysis)
    }

    fun run(emptyStep: AnalysisStep?, result: Result?): AnalysisStep? {
       asyncInitialResultRunner?.run(emptyStep, result)
       return emptyStep
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        throw Exception("InitialResultRunner does not implement getCopyDifference function.")
    }

    fun updateColumnParams(analysisStep: AnalysisStep, params: String): AnalysisStep {
        val runningStep =
            analysisStepRepository?.saveAndFlush(analysisStep.copy(status = AnalysisStepStatus.RUNNING.value))

        asyncInitialResultRunner?.updateColumnParams(analysisStep, params)

        return runningStep!!
    }

    private fun createEmptyInitialResult(analysis: Analysis?): AnalysisStep? {
        val newStep = AnalysisStep(
            status = AnalysisStepStatus.RUNNING.value,
            type = AnalysisStepType.INITIAL_RESULT.value,
            analysis = analysis,
            lastModifDate = LocalDateTime.now(),
            modifiesResult = true
        )
        return analysisStepRepository?.saveAndFlush(newStep)
    }


}
