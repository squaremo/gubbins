(ns cljs.core)

;; This is all done by an inner class in Closure's runtime, and has
;; been left out of ClojureScript.
(extend-type Subvec
  IReversible
  (-rseq [sv]
    (if (zero? (count sv))
      nil
      (RSeq. sv (dec (count sv)) nil))))
