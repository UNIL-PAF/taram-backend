package ch.unil.pafanalysis.results.service

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.service.ResultRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ResultService {

    @Autowired
    private var analysisRepository: AnalysisRepository? = null

    @Autowired
    private var resultRepository: ResultRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    fun delete(resultId: Int): Int? {
        val analysisList = analysisRepository?.findByResultId(resultId)
        analysisList?.forEach{analysisService?.delete(it.id!!)}
        return resultRepository?.deleteById(resultId)
    }

}