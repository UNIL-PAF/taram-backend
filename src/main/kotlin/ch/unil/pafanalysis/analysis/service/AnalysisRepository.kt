package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.Analysis
import org.springframework.data.repository.CrudRepository

interface AnalysisRepository: CrudRepository<Analysis, Integer> {
    fun findByResultId(resultId: Int): List<Analysis>
}
