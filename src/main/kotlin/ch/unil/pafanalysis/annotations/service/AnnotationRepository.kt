package ch.unil.pafanalysis.annotations.service

import ch.unil.pafanalysis.annotations.model.AnnotationInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface AnnotationRepository: JpaRepository<AnnotationInfo, Integer> {
    fun findById(id: Int): AnnotationInfo

    @Modifying
    @Query("delete from AnnotationInfo a where a.id =:id")
    fun deleteById(id: Int): Int?
}