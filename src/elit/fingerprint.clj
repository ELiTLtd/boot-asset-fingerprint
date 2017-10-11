(ns elit.fingerprint
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.io File]))

(def asset-regex #"\$\{(.+?)\}")
(def file-path-regex #"(.*)\.([^.]*?)$")
(def separator-char (first (File/separator)))
(def leading-slash-regex (re-pattern (str "^" separator-char)))

(defn add-leading-slash
  [path]
  (cond->> path
    (not= separator-char (first path))
    (str separator-char)))

(defn remove-leading-slash
  [path]
  (string/replace-first path leading-slash-regex ""))

(defn add-trailing-slash
  [path]
  (cond-> path
    (not= separator-char (last path))
    (str separator-char)))

(defn fingerprint-file-path
  [path hash]
  (string/replace path file-path-regex (str "$1-" hash ".$2")))

(defn remove-asset-root
  [asset-ref asset-root]
  (string/replace-first asset-ref
                        (re-pattern (str asset-root separator-char)) ""))

(defn prepend-asset-host
  [asset-ref {:keys [asset-host asset-root]}]
  (cond->> asset-ref
    asset-host
    (str (add-trailing-slash asset-host))))

(defn normalise-asset-ref
  [asset-ref asset-root]
  (if asset-root
    (->> (add-leading-slash asset-ref)
         (str asset-root))
    (remove-leading-slash asset-ref)))

(defn find-asset-refs
  [file-text {:keys [asset-root]}]
  (->> (re-seq asset-regex file-text)
       (map (comp #(normalise-asset-ref % asset-root)
                  second))))

(defn replacer
  [path {:keys [asset-root path->file skip? strict?] :as opts}]
  (if-not skip?
    (if-let [{:keys [hash]} (->> (normalise-asset-ref path asset-root)
                                 (get path->file))]
      (-> path
          (fingerprint-file-path hash)
          (remove-leading-slash)
          (prepend-asset-host opts))
      (if strict?
        (throw
         (ex-info "Could not find asset in fileset"
                  {:path path}))
        path))
    path))

(defn update-text
  [file-text opts]
  (string/replace file-text asset-regex (comp #(replacer % opts)
                                              second)))
