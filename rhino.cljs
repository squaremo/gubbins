;; Use Rhino's FFI to Java to get some stuff we need.  Yes: Clojure,
;; compiled to JavaScript, that uses the JavaScript runtime's FFI to
;; Java.

(ns gubbins.rhino)

;; Give a URL, get a string.
;; Utterly delicious and skangey scanner hack HT
;; http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
(defn slurp [urlstr]
  (let* [url (js/java.net.URL. urlstr)
         stream (.openStream url)]
        (-> stream (js/java.util.Scanner.) (.useDelimiter "\\A") (.next))))

;; Ultimately we want every resource to look like it returns a
;; relation, i.e., a sequence of like values. Here we cheat a bit by
;; simply wrapping anything that's not a (JSON) list in a seq.
(defn fetch [url]
  (let [res (js->clj (.parse js/JSON (slurp url)))]
    (if (list? res) res (list res))))
