package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.results.service.ResultRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AnalysisService {

    @Autowired
    private var analysisRepository: AnalysisRepository? = null

    @Autowired
    private var resultRepository: ResultRepository? = null

    private fun createNewAnalysis(resultId: Int): List<Analysis>?{
        val newAnalysis = Analysis(idx=0, resultId = resultId, lastModifDate = LocalDateTime.now())
        val analysisList =  listOf(analysisRepository?.save(newAnalysis))
        return if (analysisList.any { it == null }) null else analysisList as List<Analysis>
    }

    fun getByResultId(resultId: Int): List<Analysis>? {
        // check first if this result really exists
        val result = resultRepository?.findById(resultId)
        if(result == null) throw RuntimeException("There is no result for resultId [" + resultId + "]")

        val analysisInDb: List<Analysis>? = analysisRepository?.findByResultId(resultId)?.toList()

        val analysis = if(analysisInDb == null || analysisInDb!!.isEmpty()){
            createNewAnalysis(resultId)
        }else{
            analysisInDb
        }

        return analysis
    }

}