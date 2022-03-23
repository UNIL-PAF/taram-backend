package ch.unil.pafanalysis.analysis.model

import ch.unil.pafanalysis.results.model.Result
import java.time.LocalDateTime
import javax.persistence.*


@Entity
data class Analysis (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,
    val idx: Int? = null,
    val name: String? = null,
    val status: String? = null,
    val lastModifDate: LocalDateTime? = null,

    //val resultId: Int? = null,

    @OneToMany(mappedBy="analysis")
    val analysisSteps: List<AnalysisStep>? = null,

    @ManyToOne
    @JoinColumn(name="result_id", nullable=false)
    val result: Result? = null
)