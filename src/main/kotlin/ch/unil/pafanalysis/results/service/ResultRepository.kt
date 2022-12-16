package ch.unil.pafanalysis.results.service

import org.springframework.data.repository.CrudRepository
import ch.unil.pafanalysis.results.model.Result
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ResultRepository: CrudRepository<Result, Integer> {
    fun findById(resultId: Int): Result

    @Modifying
    @Query("delete from Result a where a.id =:id")
    fun deleteById(id: Int): Int?
}