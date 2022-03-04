#library("Runiversal")

run <- function(output_path){
    x <- seq(-pi,pi,0.1)

    png(paste0(output_path, "/simple_plot.png"))
    plot(x, sin(x))
    dev.off()

    Sys.sleep(10)

    return(c("done"))
}


