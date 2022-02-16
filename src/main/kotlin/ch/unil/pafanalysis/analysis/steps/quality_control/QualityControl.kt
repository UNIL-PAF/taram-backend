package ch.unil.pafanalysis.analysis.steps.quality_control

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class QualityControl {
    @Autowired
    private var analysisStepRepo: AnalysisStepRepository? = null

    var errorMessage: String? = null
    var status: String = "idle"

    fun run(): String {

        //val initialStep = AnalysisStep(resultTablePath = result?.resPath, type = "QualityControl", status = "idle", analysis = analysis, lastModifDate = LocalDateTime.now())
        //analysisStepRepo?.save(initialStep)

        this.status = "running"
        return this.status
    }

}