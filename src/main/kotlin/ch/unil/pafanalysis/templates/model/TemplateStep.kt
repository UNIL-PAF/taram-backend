package ch.unil.pafanalysis.templates.model

import com.fasterxml.jackson.annotation.JsonBackReference
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import javax.persistence.*


@Entity
data class TemplateStep (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,
    val type: String? = null,
    val lastModifDate: LocalDateTime? = null,

    @Type(type="json")
    @Column(columnDefinition="json")
    val parameters: String? = null,
    val parametersHash: Long? = null,

    @ManyToOne
    @JoinColumn(name="template_id", nullable=false)
    @JsonBackReference
    val template: Template? = null
)