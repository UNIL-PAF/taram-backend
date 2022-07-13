#library("Runiversal")

run_imputation = function(x, width = 0.3, downshift = 1.8) {
  kol.name <- as.data.frame(table(unlist(names(x))))
  kol.name <- as.character(kol.name$Var1)

  set.seed(1)
  x[kol.name] = lapply(kol.name,
                       function(y) {
                         temp = x[[y]]
                         temp[!is.finite(temp)] = NA
                         temp.sd = width * sd(temp, na.rm = TRUE)   # shrink sd width
                         temp.mean = mean(temp, na.rm = TRUE) -
                           downshift * sd(temp, na.rm = TRUE)   # shift mean of imputed values
                         n.missing = sum(is.na(temp))
                         temp[is.na(temp)] = rnorm(n.missing, mean = temp.mean, sd = temp.sd)
                         return(temp)
                       })
  return(x)

}



