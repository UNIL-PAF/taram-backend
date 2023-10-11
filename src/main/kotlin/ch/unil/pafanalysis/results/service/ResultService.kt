package ch.unil.pafanalysis.results.service

import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.results.model.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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
        analysisList?.forEach { analysisService?.delete(it.id!!) }
        return resultRepository?.deleteById(resultId)
    }

    fun setInfo(resultId: Int, name: String, description: String?): String? {
        val res = resultRepository?.findById(resultId)
        val newRes = res?.copy(name = name, description = description)
        val savedRes = resultRepository?.saveAndFlush(newRes!!)
        return savedRes.toString()
    }

}