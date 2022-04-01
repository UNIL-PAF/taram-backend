package ch.unil.pafanalysis.analysis.model

import org.hibernate.annotations.Type
import javax.persistence.*

@Entity
data class ColumnInfo (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,
    val columnMappingHash: Long? = null,

    @Type(type="json")
    @Column(columnDefinition="json")
    val columnMapping: ColumnMapping? = null,
)