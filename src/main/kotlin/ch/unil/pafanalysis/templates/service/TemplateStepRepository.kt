package ch.unil.pafanalysis.templates.service

import ch.unil.pafanalysis.templates.model.TemplateStep
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface TemplateStepRepository: CrudRepository<TemplateStep, Integer> {
    fun findById(id: Int): TemplateStep

    @Modifying
    @Query("delete from TemplateStep a where a.id =:id")
    fun deleteById(id: Int): Int?
}
