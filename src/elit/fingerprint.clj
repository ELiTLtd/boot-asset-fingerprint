(ns elit.fingerprint
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

(def asset-regex #"\$\{(.+?)\}")
(def file-path-regex #"(.*)\.([^.]*?)$")

(defn find-asset-refs
  [file-text]
  (->> (re-seq asset-regex file-text)
       (map second)))

(defn fingerprint-file-path
  [path hash]
  (string/replace path file-path-regex (str "$1-" hash ".$2")))

(defn replacer-fn
  [{:keys [skip? path->file]}]
  (fn [[_ match]]
    (if-let [{:keys [hash]} (get path->file match)]
      (cond-> match
        (not skip?)
        (fingerprint-file-path hash))
      (do (prn "error trying to replace " match)
          match))))

(defn update-text
  [file-text {:keys [extensions path->file] :as opts}]
  (string/replace file-text asset-regex (replacer-fn opts)))
