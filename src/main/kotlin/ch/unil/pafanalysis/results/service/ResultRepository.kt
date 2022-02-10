package ch.unil.pafanalysis.results.service

import ch.unil.pafanalysis.analysis.model.Analysis
import org.springframework.data.repository.CrudRepository
import ch.unil.pafanalysis.results.model.InitialResult

interface ResultRepository: CrudRepository<InitialResult, Integer> {
    fun findById(resultId: Int): InitialResult
}