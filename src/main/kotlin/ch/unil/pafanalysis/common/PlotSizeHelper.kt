package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import javax.swing.Spring.height

object PlotSizeHelper {

    fun getPlotHeigthRatio(type: String?): Double {
        val ratio =  when(type){
            AnalysisStepType.UMAP.value -> 0.8
            AnalysisStepType.PCA.value -> 0.8
            AnalysisStepType.SCATTER_PLOT.value -> 0.9
            AnalysisStepType.BOXPLOT.value -> 0.5
            AnalysisStepType.VOLCANO_PLOT.value -> 0.5
            AnalysisStepType.CORRELATION_TABLE.value -> 0.9
            else -> 0.7
        }
        return ratio
    }

    fun getPlotDimension(type: String?, width: Double, zoomFactor: Int): PlotSize {
        val height = (width * getPlotHeigthRatio(type))
        return PlotSize((width / zoomFactor), (height / zoomFactor), zoomFactor)
    }

    fun getSvgDimension(type: String?, width: Double = 2880.0, zoomFactor: Int = 1): PlotSize {
        return getPlotDimension(type, width, zoomFactor)
    }

    fun getPngDimension(type: String?, width: Double = 3000.0, zoomFactor: Int = 1): PlotSize{
        return getPlotDimension(type, width, zoomFactor)
    }

    fun getDefaultPngDimension(type: String?): PlotSize {
        val width = 3000.0
        val zoom = when(type){
            AnalysisStepType.UMAP.value -> 5
            AnalysisStepType.PCA.value -> 5
            AnalysisStepType.SCATTER_PLOT.value -> 4
            AnalysisStepType.BOXPLOT.value -> 3
            AnalysisStepType.VOLCANO_PLOT.value -> 3
            AnalysisStepType.CORRELATION_TABLE.value -> 4
            else -> 1
        }
        return getPngDimension(type, width, zoom)
    }

    data class PlotSize(val width: Double, val height: Double, val zoom: Int)
}