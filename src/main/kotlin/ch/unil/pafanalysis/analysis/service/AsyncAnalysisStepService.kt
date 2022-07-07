package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

@Service
class AsyncAnalysisStepService {

    @Autowired
    private var initialResultRunner: InitialResultRunner? = null

    @Autowired
    private var commonStep: CommonStep? = null

    @Async
    fun runDuplicatedSteps(emptyInitialStep: AnalysisStep?, analysisSteps: List<AnalysisStep>, newAnalysis: Analysis) {
        if (analysisSteps != null && analysisSteps.size > 1) {
            initialResultRunner?.run(emptyStep = emptyInitialStep, newAnalysis.result)
            commonStep?.updateNextStep(analysisSteps[0]!!)
        }
    }


}