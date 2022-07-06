package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface AnalysisStepRepository: JpaRepository<AnalysisStep, Integer> {
    fun findById(id: Int): AnalysisStep

    @Modifying
    @Query("delete from AnalysisStep a where a.id =:id")
    fun deleteById(id: Int): Int?

    @Modifying
    @Query("update AnalysisStep a set a.status =:status where a.id =:stepId")
    fun setStatusById(@Param("status") status: String, @Param("stepId") id: Int): Int
}
