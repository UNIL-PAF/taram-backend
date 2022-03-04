package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import ch.unil.pafanalysis.analysis.steps.quality_control.QualityControlRunner
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.service.ResultRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
@Service
class AnalysisStepService {

    @Autowired
    private var analysisStepRepo: AnalysisStepRepository? = null

    fun setAnalysisStepStatus(id: Int, status: AnalysisStepStatus): Int?{
        return analysisStepRepo?.setStatusById(status.value, id)
    }

}