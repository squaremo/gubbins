(ns gubbins.query
  ;; (:require-macros [cljs.core.logic.macros :as l]))
  (:require
   ;;[cljs.core.logic :as logic]
   [clojure.set :as set]
   [clojure.string :as string]
   [clojure.math.combinatorics :as combos]
   ))

;; Queries

(defn outputs [ [_inputs outs] ]
  (set outs))

(defn inputs [ [inputs _outputs] ]
  (set inputs))

;; Sources

(defn source-template [ [_rel templ _attrs] ]
  templ)

(defn source-attrs [ [_rel _url attrs] ]
  attrs)

(defn attr-names [attrs]
  (set (map :name attrs)))

(def bound? :bound)

(defn bound-attrs [source]
  (attr-names (filter bound? (source-attrs source))))

;; Connections
;; (which are ..)

;; a <= b
(defn contained? [a b]
  (let [a (set a)
        b (set b)]
    (or (== a b)
        (set/subset? a b))))

;; %%% TODO: can the query supply values for any of the sources; in
;; %%% other words, for each source the requirements are met either by
;; %%% a query input or by the previous join.

;; In a natural join, S1 ... Sn, the bound attributes are those
;; required by S1, and the outputs are the union of the attributes in
;; all Si.

;; NB may be worth calculating all these sets once, since we use them
;; a few times.

(defn required-in [join]
  (bound-attrs (first join)))

(defn provided-by [join]
  (apply set/union (map #(attr-names (source-attrs %)) join)))

(defn join-attrs [a b]
  (set/intersection (attr-names (source-attrs a)) (attr-names (source-attrs b))))

;; We can make a join if 1. there's attributes in common between the
;; first two sources 2. the left-most source supplies the
;; requirements of the right-hand join.
(defn join? [relations]
  (let [f (first relations)
        r (rest relations)]
    (cond (empty? r)
          true
          :else (and (contained? (required-in r) (provided-by [f]))
                     (not (empty? (join-attrs f (first r))))
                     (join? r)))))

#_
(join?
 [ ['a "whatever/a" [{:name "foo" :bound true}
                     {:name "bar"}]]
   ['b "whatever/b" [{:name "bar" :bound true}]] ])

        
(defn covers? [join query]
  (and
   (contained? (required-in join) (inputs query))
   (contained? (outputs query) (provided-by join))))

;; Blithely go and calculate which joins will give the answers. As a
;; lazy sequence.
(defn connections [sources query]
  (filter #(and (join? %) (covers? % query))
          (mapcat combos/permutations (combos/subsets sources))))

(defn plan [join query]
  (cond (empty? (rest join)) (plan-source (first join) query)
        :else (plan-join join query)))

(defn plan-source [source query]
  (let* [template (source-template source)
         template-args (required-in [source])
         restrict-args (set/difference (inputs query) template-args)
         project-args (outputs query)]
        [{:template template
          :arguments template-args
          :restrict restrict-args
          :project project-args}]))

;; For a join we will in general have to take values from each source
;; to use in the next source.

(defn plan-join [join query]
  (let [f (first join)
        r (rest join)]
    (cons {:template (source-template f)
           :arguments (bound-attrs f)
           ;; The inputs may be used elsewhere, we just want those
           ;; that are in this source
           :restrict (set/intersection
                      (attr-names (source-attrs f))
                      (set/difference (inputs q) (bound-attrs f)))
           ;; For this we need anything in the final output, as well as anything
           ;; needed in the join
           :project (set/intersection (attr-names (source-attrs f))
                                      (set/union (outputs query) (required-in r)))}
          (plan r query))))

;; TODO apply aliases (after filtering if we can, and only if necessary)

;; Execute a plan. This turns a plan into a procedure that accepts a
;; fetch function, and the input parameters, and yields results.
(defn prepare-plan [plan]
  (fn [fetch initial-args]
    (reduce (fn [results, {template :template
                           _ :arguments
                           restrict :restrict
                           project :project}]
              (mapcat
               (fn [args]
                 (let* [url (instantiate-template template args)
                        restrict-vals (select-keys args restrict)
                        filterer #(= (select-keys % restrict) restrict-vals)
                        res (fetch url)]
                       (->> res (filter filterer) (map #(select-keys (merge args %) project)))))
               results)) [initial-args] plan)))


(defn instantiate-template [template args]
  (reduce (fn [s key] (string/replace s (str "{" key "}") (args key)))
          template (keys args)))

;; Example

(def sources
  [['repo "https://api.github.com/repos/{login}/{name}" 
    [{:name "name", :bound true}
     {:name "full_name"}
     {:name "login", :bound true}]]
   ['repo "https://api.github.com/repos/{full_name}"
    [{:name "full_name", :bound true}
     {:name "name"}
     {:name "login", :alias #(-> % "owner" "login")}]]
   ['repo "https://api.github.com/users/{login}/repos"
    [{:name "login", :bound true}
     {:name "name"}
     {:name "full_name"}]]
   ['watcher "https://api.github.com/repos/{login}/{name}/watchers"
    [{:name "login" :bound true}
     {:name "name" :bound true}
     {:name "avatar_url"}]]
   ['user "https://api.github.com/users/{login}"
    [{:name "login", :bound true}
     {:name "email"}]]])

(def query
  [ ["name" "login"] ["full_name"]])

(def query2
  [ ["full_name"] ["name" "login"] ])

(def query3
  [ ["login"], ["login" "name" "avatar_url"] ])

(def DB
  {"https://api.github.com/repos/squaremo/bitsyntax-js"
   [{"name" "bitsyntax-js"
     "login" "squaremo"
     "full_name" "squaremo/bitsyntax-js"}]
   "https://api.github.com/repos/squaremo/rabbit.js"
   [{"name" "rabbit.js"
     "login" "squaremo"
     "full_name" "squaremo/rabbit.js"}]
   "https://api.github.com/users/squaremo/repos"
   [{"name" "bitsyntax-js"
     "login" "squaremo"
     "full_name" "squaremo/bitsyntax-js"}
    {"name" "rabbit.js"
     "login" "squaremo"
     "full_name" "squaremo/rabbit.js"}]
   "https://api.github.com/repos/squaremo/rabbit.js/watchers"
   [{"avatar_url" "http://one.thing.com/"}
    {"avatar_url" "http://and.another.thing"}]
   "https://api.github.com/repos/squaremo/bitsyntax-js/watchers"
   [{"avatar_url" "http://some.thing.com/"}
    {"avatar_url" "http://some.other.thing.com"}]})

(defn db-fetch [url]
  (DB url))

(def pl (plan (first (connections sources query)) query))

(def ex (prepare-plan pl))

(ex db-fetch {"name" "bitsyntax-js", "login" "squaremo"})

;; ;; Inner bit of prepare-plan
;; (let [fetch db-fetch,
;;       {template :template
;;        _ :arguments
;;        restrict :restrict
;;        project :project} (first pl)]
;;   (mapcat
;;    (fn [args]
;;      (let* [url (instantiate-template template args)
;;             restrict-vals (select-keys args restrict)
;;             filterer #(= (select-keys % restrict) restrict-vals)
;;             res (fetch url)]
;;            (->> res (filter filterer) (map #(select-keys (merge args %) project)))))
;;    [{"login" "squaremo", "name" "rabbit.js"}]))
