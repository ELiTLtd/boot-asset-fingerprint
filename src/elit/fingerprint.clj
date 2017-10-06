(ns elit.fingerprint
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

(def asset-regex #"\$\{(.+?)\}")

(defn find-asset-paths
  [file-text]
  (->> (re-seq asset-regex file-text)
       (map second)))

(defn append-hash
  [path hash]
  (str path hash))

(defn update-refs
  [file-text path->file]
  (string/replace file-text
                  asset-regex
                  (fn [[_ match]]
                    (if-let [{:keys [hash]} (get path->file match)]
                      (append-hash match hash)
                      (do (prn "error trying to replace " match)
                          match)))))

(defn fingerprint*
  [{:keys [path hash file-text] :as file} path->file {:keys [extensions]}]
  {:file (update file :file-text update-refs path->file)
   :file-asset-paths (doto (find-asset-paths file-text) prn)})

(defn fingerprint
  [{:keys [path] :as file} file->map opts]
  (fingerprint* (assoc file :file-text (slurp (io/resource path)))
                file->map
                opts))
