package ch.unil.pafanalysis.analysis.model

import java.time.LocalDateTime
import javax.persistence.*

@Entity
class Analysis (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,
    val idx: Int? = null,
    val resultId: Int? = null,
    val name: String? = null,
    val status: String? = null,
    val lastModifDate: LocalDateTime? = null,

    @OneToMany(mappedBy="analysis")
    val analysisSteps: Set<AnalysisStep>? = null
)