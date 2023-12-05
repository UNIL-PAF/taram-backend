package ch.unil.pafanalysis.annotations.model

import org.hibernate.annotations.Type
import java.time.LocalDateTime
import javax.persistence.*

@Entity
data class AnnotationInfo (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,
    val name: String? = null,
    @Lob
    val description: String? = null,
    @Lob
    val usedBy: String? = null,
    @Type(type="json")
    @Column(columnDefinition="json")
    val headers: List<AnnotationHeader>? = null,
    val fileName: String? = null,
    val origFileName: String? = null,
    val nrEntries: Int? = null,
    val creationDate: LocalDateTime? = null
)


data class AnnotationHeader(
    val id: Int? = null,
    val name: String? = null
)

