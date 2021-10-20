package ch.unil.pafanalysis.results.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id;

@Entity
class InitialResult (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,
    val name: String? = null
    // var date
    //val type: String? = null
    //val resFile: String? = null
)