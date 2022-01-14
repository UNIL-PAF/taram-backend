package ch.unil.pafanalysis.results.model

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id;

@Entity
class InitialResult (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,
    val name: String? = null,
    val type: String? = null,
    val status: String? = null,
    val resFile: String? = null,
    val fileCreationDate: LocalDateTime? = null,
    val lastModifDate: LocalDateTime? = null
)