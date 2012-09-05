(ns gubbins.query
  (:require ;[cljs.core.logic :as logic]
   [clojure.set :as set]
   [clojure.string :as string]
   ))
;  (:require-macros [cljs.core.logic.macros :as l]))

;; Queries

(defn outputs [ [_rel _inputs outs] ]
  (set outs))

(defn inputs [ [_rel inputs _outputs] ]
  (set inputs))

(defn relation [ [rel &_] ]
  rel)

;; Sources

(defn source-template [ [_rel templ _attrs] ]
  templ)

(defn source-attrs [ [_rel _url attrs] ]
  attrs)

(defn attr-names [source]
  (set (map :name (source-attrs source))))

(def bound? :bound)

(defn required-in [source]
  (set (map :name (filter bound? (source-attrs source)))))

;; Connections
;; (which are ..)

(defn contained? [a b]
  (let [a (set a)
        b (set b)]
    (or (== a b)
        (set/subset? a b))))

;; (defn diff [a b]
;;   (set/difference (set a) (set b)))

(defn covers? [source query]
  (and
   (== (relation source) (relation query))
   (contained? (required-in source) (inputs query))
   (contained? (outputs query) (attr-names source))))


;; A trivial join (i.e., a single source) S for query Q has
;;
;;     inputs(Q) >= required(S) and outputs(Q) <= supplied(S)
;;
;; as well as matching the relation queried:
;;
;;     relation(S) = relation(Q)

;; %%% TODO do we need to name the relation in the query, or just the
;; %%% output attributes (and assume natural joins)

;; (NB listifies everything because they are degenerate joins)
(defn connections [sources query]
  (map list (filter #(covers? % query) sources)))

(defn plan [connection query]
  (cond (empty? (rest connection)) (plan-source (first connection) query)
        :else (plan-join connection query)))

(defn plan-source [source query]
  (let* [template (source-template source)
         template-args (required-in source)
         restrict-args (set/difference (inputs query) template-args)
         project-args (outputs query)]
        {:template template
         :arguments template-args
         :restrict restrict-args
         :project project-args}))

;; TODO plan-join

;; TODO apply aliases (after filtering if we can, and only if necessary)

;; Execute a plan. This turns a plan into a procedure that accepts a
;; fetch function, and the input parameters, and yields results.
(defn prepare-plan [{template :template
                     _ :arguments
                     restrict :restrict
                     project :project}]
  (fn [fetch args]
    (let* [url (instantiate-template template args)
           restrict-vals (select-keys args restrict)
           filterer #(= (select-keys % restrict) restrict-vals)
           res (fetch url)]
          (->> res (filter filterer) (map #(select-keys % project))))))


(defn instantiate-template [template args]
  (reduce (fn [s key] (string/replace s (str "{" key "}") (args key)))
          template (keys args)))

;; Example

(def sources
  [['repo "/{user.login}/{repo.name}" 
    [{:name 'repo.name, :bound true}
     {:name 'repo.full_name}
     {:name 'user.login, :bound true}]]
   ['repo "/{repo.full_name}"
    [{:name 'repo.full_name, :bound true}
     {:name 'repo.name, :alias :name}
     {:name 'user.login, :alias #(-> % :owner :login)}]]])

(def query
  ['repo ['repo.name 'user.login] ['repo.full_name]])

(def query2
  ['repo ['repo.full_name] ['repo.name 'user.login] ])

(def DB
  {"/squaremo/bitsyntax-js"
   [{'repo.name "bitsyntax-js"
     'user.login "squaremo"
     'repo.full_name "squaremo/bitsyntax-js"}]
   "/squaremo/rabbit.js"
   [{'repo.name "rabbit.js"
     'user.login "squaremo"
     'repo.full_name "squaremo/rabbit.js"}]})

(defn db-fetch [url]
  (DB url))

(def pl (plan (first (connections sources query)) query))

(def ex (prepare-plan pl))

(x db-fetch {'repo.name "bitsyntax-js", 'user.login "squaremo"})
