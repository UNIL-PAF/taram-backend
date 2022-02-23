package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.results.model.Result
import org.springframework.data.repository.CrudRepository

interface AnalysisRepository: CrudRepository<Analysis, Integer> {
    fun findByResultId(resultId: Int): List<Analysis>
    fun findById(analysisId: Int): Analysis
}
