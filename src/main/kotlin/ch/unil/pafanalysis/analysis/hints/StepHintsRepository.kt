package ch.unil.pafanalysis.analysis.hints

import ch.unil.pafanalysis.analysis.model.StepHints
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface StepHintsRepository: JpaRepository<StepHints, Integer> {
    @Modifying
    @Query("delete from StepHints a where a.id =:id")
    fun deleteById(id: Int): Int?

    fun findByResultId(resultId: Int): List<StepHints>
    fun findById(stepHintsId: Int): StepHints
}
