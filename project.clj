(defproject gubbins "1.0.0-SNAPSHOT"

  ;; This left in mainly to fetch all the ClojureScript machinery and provide a REPL
  :plugins [[lein-cljsbuild "0.2.5"]]

  ;; :cljsbuild {
  ;;             ;; :crossovers [clojure.contrib.datalog
  ;;             ;;              clojure.contrib.seq
  ;;             ;;              clojure.contrib.def
  ;;             ;;              clojure.contrib.except
  ;;             ;;              clojure.contrib.graph
  ;;             ;;              clojure.contrib.set]
  ;;             ;; :crossover-path "cljs-src"
  ;;             :builds [
  ;;                      {
  ;;                       :source-path "cljs-src"
  ;;                       :compiler {
  ;;                                  :output-to "out/lib.js"  ; default: main.js in current directory
  ;;                                  :optimizations :simple
  ;;                                  :target :nodejs
  ;;                                  :pretty-print true}}
  ;;                      ;; {
  ;;                      ;;  ;; The path to the top-level ClojureScript source directory:
  ;;                      ;;  :source-path "src"
  ;;                      ;;  ;; The standard ClojureScript compiler options:
  ;;                      ;;  ;; (See the ClojureScript compiler documentation for details.)
  ;;                      ;;  :compiler {
  ;;                      ;;             :output-to "out/main.js"  ; default: main.js in current directory
  ;;                      ;;             :optimizations :simple
  ;;                      ;;             :target :nodejs
  ;;                      ;;             :pretty-print true}}]}
  ;;                      ]}
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 #_ [org.clojure/data.json "0.1.3"]
                 #_ [org.clojure/core.logic "0.7.5"]])
