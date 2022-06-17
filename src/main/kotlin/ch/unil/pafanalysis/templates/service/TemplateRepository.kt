package ch.unil.pafanalysis.templates.service

import ch.unil.pafanalysis.templates.model.Template
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface TemplateRepository: CrudRepository<Template, Integer> {
    @Modifying
    @Query("delete from Template a where a.id =:id")
    fun deleteById(id: Int): Int?

    fun findByName(name: String): List<Template>
    fun findById(templateId: Int): Template
}
