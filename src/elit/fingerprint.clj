(ns elit.fingerprint
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.io File]))

(def asset-regex #"\$\{(.+?)\}")
(def file-path-regex #"(.*)\.([^.]*?)$")
(def seperator-char (first (File/separator)))

(defn fingerprint-file-path
  [path hash]
  (string/replace path file-path-regex (str "$1-" hash ".$2")))

(defn drop-trailing-slash
  [path]
  (apply str (cond->> path
               (= seperator-char (last path))
               butlast)))

(defn with-leading-slash
  [path]
  (cond->> path
    (not= seperator-char (first path))
    (str seperator-char)))

(defn prepend-asset-host
  [asset-host]
  (if asset-host
    #(str (drop-trailing-slash asset-host) %)
    identity))

(defn remove-asset-root
  [asset-root]
  (if asset-root
    #(string/replace-first % (re-pattern (str asset-root seperator-char)) "")
    identity))

(defn normalise-asset-ref
  [asset-ref asset-root]
  (cond->> (with-leading-slash asset-ref)
    asset-root (str asset-root)))

(defn find-asset-refs
  [file-text {:keys [asset-root]}]
  (->> (re-seq asset-regex file-text)
       (map (comp #(normalise-asset-ref % asset-root)
                  second))))

(defn replacer-fn
  [{:keys [asset-root asset-host path->file skip? strict?]}]
  (fn [match]
    (if-not skip?
      (if-let [{:keys [hash]} (get path->file (normalise-asset-ref match asset-root))]
        ((comp (prepend-asset-host asset-host)
               (remove-asset-root asset-root)
               #(fingerprint-file-path % hash))
         match)
        (if strict?
          (throw
           (ex-info "Could not find asset in fileset"
                    {:match match}))
          match))
      match)))

(defn update-text
  [file-text opts]
  (string/replace file-text asset-regex (comp (replacer-fn opts)
                                              second)))