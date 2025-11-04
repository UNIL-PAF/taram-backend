package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AnalysisStepRepository: JpaRepository<AnalysisStep, Integer> {
    fun findById(id: Int): AnalysisStep?

    @Query("SELECT a.status FROM analysis_step a WHERE a.id = :id", nativeQuery = true)
    fun getStepStatusById(@Param("id") analysisStepId: Int?): String?

    @Query("SELECT * FROM analysis_step a WHERE a.id = :id", nativeQuery = true)
    fun getStepById(@Param("id") id: Int?): AnalysisStep?

    fun existsById(id: Int): Boolean

    @Modifying
    @Query("delete from AnalysisStep a where a.id =:id")
    fun deleteById(id: Int): Int?

    @Modifying
    @Query("update AnalysisStep a set a.status =:status where a.id =:stepId")
    fun setStatusById(@Param("status") status: String, @Param("stepId") id: Int): Int
}
