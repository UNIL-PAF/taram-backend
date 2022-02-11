package ch.unil.pafanalysis.analysis.model

import java.time.LocalDateTime
import javax.persistence.*

@Entity
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

    @Column(columnDefinition="json")
    val

    @ManyToOne
    @JoinColumn(name="analysis_id", nullable=false)
    private val analysis: Analysis? = null

)