(ns elit.boot-asset-fingerprint
  {:boot/export-tasks true}
  (:require [boot.core :as boot]
            [clojure.java.io :as io]
            [elit.fingerprint :as fingerprint]))

(defprotocol DirWriter
  (update-file! [this path contents])
  (copy-file! [this file dest-path]))

(defrecord TmpDirWriter [out-dir]
  DirWriter
  (update-file! [_ path contents]
    (prn "updating file text!" path "to" contents))
  (copy-file! [_ src-path dest-path]
    (prn "copying" src-path "to " dest-path)))

(defn asset-fingerprint*
  [files out-dir {:keys [asset-root asset-host extensions path->file skip?] :as opts}]
  (let [file-writer (->TmpDirWriter out-dir)]
    (loop [[{:keys [path] :as file} :as content-files] files
           asset-paths []]
      (if file
        (let [file-text (slurp (io/resource path))
              updated-file-text (fingerprint/update-text file-text
                                                         {:extensions extensions
                                                          :path->file path->file})]
          (update-file! file-writer path updated-file-text)
          (recur (rest content-files)
                 (concat asset-paths (fingerprint/find-asset-refs file-text))))
        (when-not skip?
          (prn "asset-paths: " asset-paths)
          (doseq [asset-path asset-paths]
            (let [{:keys [path hash] :as file} (get path->file asset-path)]
              (copy-file! file-writer path (fingerprint/fingerprint-file-path asset-path hash)))))))))

(boot/deftask asset-fingerprint
  []
  (let [extensions [".html" ".css"]
        opts {:skip? false :extensions extensions}
        out-dir (boot/tmp-dir!)]
    (boot/with-pre-wrap fileset
      (let [files (boot/input-files fileset)
            path->file (->> (map (juxt :path identity) files)
                            (into {}))]
        (asset-fingerprint* (boot/by-ext extensions files)
                            out-dir
                            (assoc opts :path->file path->file))
        (-> fileset
            (boot/add-resource out-dir)
            (boot/commit!))))))
