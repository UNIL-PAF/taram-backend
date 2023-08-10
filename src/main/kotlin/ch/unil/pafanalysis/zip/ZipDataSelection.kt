package ch.unil.pafanalysis.zip

data class ZipDataSelection (
  val plots: List<Int>? = null,
  val tables: List<Int>? = null,
  val steps: List<Int>? = null,
  val analysisId: Int? = null
)