package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.results.model.Result
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface AnalysisRepository: JpaRepository<Analysis, Integer> {
    @Modifying
    @Query("delete from Analysis a where a.id =:id")
    fun deleteById(id: Int): Int?

    fun findByResultId(resultId: Int): List<Analysis>
    fun findById(analysisId: Int): Analysis
}
