package ch.unil.pafanalysis.results.service

import ch.unil.pafanalysis.results.model.Result
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ResultRepository: JpaRepository<Result, Integer> {
    fun findById(resultId: Int): Result

    @Modifying
    @Query("delete from Result a where a.id =:id")
    fun deleteById(id: Int): Int?
}