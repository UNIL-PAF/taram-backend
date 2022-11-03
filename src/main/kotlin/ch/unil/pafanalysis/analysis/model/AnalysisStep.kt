package ch.unil.pafanalysis.analysis.model

import ch.unil.pafanalysis.analysis.steps.CommonResult
import com.fasterxml.jackson.annotation.JsonBackReference
import com.vladmihalcea.hibernate.type.json.JsonType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@TypeDef(name = "json", typeClass = JsonType::class)
data class AnalysisStep (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,
    val nextId: Int? = null,
    val beforeId: Int? = null,
    val comments: String? = null,
    val status: String? = null,
    val type: String? = null,
    val lastModifDate: LocalDateTime? = null,
    val error: String? = null,
    val stepHash: Long? = null,
    val modifiesResult: Boolean? = null,

    val resultPath: String? = null,
    val resultTablePath: String? = null,
    val imputationTablePath: String? = null,
    val resultTableHash: Long? = null,
    val tableNr: Int? = null,

    val copyFromId: Int? = null,
    val copyDifference: String? = null,

    @Type(type="json")
    @Column(columnDefinition="json")
    val commonResult: CommonResult? = null,

    @Type(type="json")
    @Column(columnDefinition="json")
    val parameters: String? = null,

    val commonHash: Long? = null,
    val parametersHash: Long? = null,

    @ManyToOne
    @JoinColumn(name="column_info_id")
    val columnInfo: ColumnInfo? = null,

    @Type(type="json")
    @Column(columnDefinition="json")
    val results: String? = null,

    @ManyToOne
    @JoinColumn(name="analysis_id", nullable=false)
    @JsonBackReference
    val analysis: Analysis? = null

)