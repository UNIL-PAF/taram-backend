package ch.unil.pafanalysis.results.service

import org.springframework.data.repository.CrudRepository
import ch.unil.pafanalysis.results.model.Result

interface ResultRepository: CrudRepository<Result, Integer> {
    fun findById(resultId: Int): Result
}