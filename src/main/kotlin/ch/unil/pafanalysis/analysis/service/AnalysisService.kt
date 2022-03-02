package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStatus
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import ch.unil.pafanalysis.analysis.steps.quality_control.QualityControlRunner
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.service.ResultRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AnalysisService {

    @Autowired
    private var analysisRepo: AnalysisRepository? = null

    @Autowired
    private var resultRepo: ResultRepository? = null

    @Autowired
    private var initialResult: InitialResultRunner? = null

    private fun createNewAnalysis(result: Result?): List<Analysis>?{

        val newAnalysis = Analysis(idx=0, resultId = result?.id, lastModifDate = LocalDateTime.now(), status = AnalysisStatus.CREATED.value)
        val analysis: Analysis? = analysisRepo?.save(newAnalysis)

        initialResult?.run(analysis?.id, result)

        val analysisList =  listOf(analysis)
        return if (analysisList.any { it == null }) null else analysisList as List<Analysis>
    }

    fun getByResultId(resultId: Int): List<Analysis>? {
        // check first if this result really exists
        val result = resultRepo?.findById(resultId)
        if(result == null) throw RuntimeException("There is no result for resultId [" + resultId + "]")

        val analysisInDb: List<Analysis>? = analysisRepo?.findByResultId(resultId)?.toList()

        val analysis = if(analysisInDb == null || analysisInDb!!.isEmpty()){
            createNewAnalysis(result)
            analysisRepo?.findByResultId(resultId)?.toList()
        }else{
            analysisInDb
        }

        return analysis
    }

}