package ch.unil.pafanalysis.analysis.model

import com.vladmihalcea.hibernate.type.json.JsonType
import org.hibernate.annotations.TypeDef
import javax.persistence.*

@Entity
@TypeDef(name = "json", typeClass = JsonType::class)
data class StepHints (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,

    val resultId: Int? = null,

    @Lob
    val hintsDone: String? = null,
)

data class StepHintInfo(
    val hintList: List<StepHint>? = null,
    val nextHintId: String? = null
)

data class StepHint(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val isDone: Boolean? = null
)

