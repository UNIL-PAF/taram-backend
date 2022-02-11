package ch.unil.pafanalysis.analysis.model

import com.vladmihalcea.hibernate.type.json.JsonType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@TypeDef(name = "json", typeClass = JsonType::class)
class AnalysisStep (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,
    val beforeId: Int? = null,
    val afterId: Int? = null,
    val comments: String? = null,
    val resultTablePath: String? = null,
    val status: String? = null,
    val type: String? = null,
    val lastModifDate: LocalDateTime? = null,

    @Type(type="json")
    @Column(columnDefinition="json")
    val columnMapping: ColumnMapping? = null,

    @ManyToOne
    @JoinColumn(name="analysis_id", nullable=false)
    private val analysis: Analysis? = null

)