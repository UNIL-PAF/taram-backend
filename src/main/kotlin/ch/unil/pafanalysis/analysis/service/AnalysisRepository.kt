package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.Analysis
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface AnalysisRepository: JpaRepository<Analysis, Integer> {
    @Modifying
    @Query("delete from Analysis a where a.id =:id")
    fun deleteById(id: Int): Int?

    fun findByResultId(resultId: Int): List<Analysis>
    fun findById(analysisId: Int): Analysis
}
