package ch.unil.pafanalysis.templates.model

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import com.fasterxml.jackson.annotation.JsonManagedReference
import java.time.LocalDateTime
import javax.persistence.*


@Entity
data class Template (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,
    val name: String? = null,
    val lastModifDate: LocalDateTime? = null,

    @OneToMany(mappedBy="template", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @JsonManagedReference
    val templateSteps: List<TemplateStep>? = null
)