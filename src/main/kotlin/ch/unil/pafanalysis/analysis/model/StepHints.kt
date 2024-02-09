package ch.unil.pafanalysis.analysis.model

import ch.unil.pafanalysis.results.model.Result
import com.vladmihalcea.hibernate.type.json.JsonType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import javax.persistence.*

@Entity
@TypeDef(name = "json", typeClass = JsonType::class)
data class StepHints (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,

    @OneToOne
    @JoinColumn(name="result_id", nullable=false)
    val result: Result? = null,

    @Type(type="json")
    @Column(columnDefinition="json")
    val hintInfo: StepHintInfo? = null,
)

data class StepHintInfo(
    val hintList: List<StepHint>? = null,
    val nextHintId: Int? = null
)

data class StepHint(
    val id: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val isDone: Boolean? = null
)