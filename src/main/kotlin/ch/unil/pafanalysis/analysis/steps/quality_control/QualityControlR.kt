package ch.unil.pafanalysis.analysis.steps.quality_control

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.service.AnalysisStepService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.concurrent.thread

//@Transactional
@Service
class QualityControlR {
    @Autowired
    private var analysisStepService: AnalysisStepService? = null

    fun runR(analysisStep: AnalysisStep){
        thread(start = true, isDaemon = true) {

            // lets sleep for 30 seconds
            Thread.sleep(30_000)

            // update analysisStep status to Done
            analysisStepService?.setAnalysisStepStatus(analysisStep.id!!, AnalysisStepStatus.DONE)
        }

    }

}

/*

        val step: AnalysisStep = try {
            val initialResult = createInitialResult(maxQuantPath)

            AnalysisStep(
                resultTablePath = newTable.name,
                status = AnalysisStepStatus.DONE.value,
                type = type,
                analysis = analysis,
                lastModifDate = lastModif,
                results = gson.toJson(initialResult)
            )
        } catch (e: StepException) {
            AnalysisStep(
                status = AnalysisStepStatus.ERROR.value,
                type = type,
                error = e.message,
                analysis = analysis,
                lastModifDate = lastModif
            )
        }
*/