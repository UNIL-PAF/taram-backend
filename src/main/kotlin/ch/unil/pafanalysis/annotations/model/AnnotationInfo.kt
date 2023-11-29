package ch.unil.pafanalysis.annotations.model

import ch.unil.pafanalysis.results.model.Result
import com.fasterxml.jackson.annotation.JsonManagedReference
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
    @Lob
    val headers: String? = null,
    val fileName: String? = null,
    val origFileName: String? = null,
    val nrEntries: Int? = null,
    val creationDate: LocalDateTime? = null
)