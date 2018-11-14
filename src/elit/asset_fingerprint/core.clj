(ns elit.asset-fingerprint.core
  (:require [clojure.java.io :as io]
            [elit.asset-fingerprint.dir-writer :as writer]
            [elit.asset-fingerprint.fingerprint :as fingerprint]))

(defn asset-fingerprint
  [files out-dir {:keys [asset-root asset-host path->file skip?] :as opts}]
  (let [file-writer (writer/->TmpDirWriter out-dir)]
    (loop [[{:keys [path] :as file} & more-content-files] files
           asset-paths []]
      (if file
        (let [file-text (slurp (io/resource path))
              updated-file-text (fingerprint/update-text file-text opts)]
          (writer/update-file! file-writer path updated-file-text)
          (recur more-content-files
                 (concat asset-paths (fingerprint/find-asset-refs file-text opts))))
        (when-not skip?
          (doseq [asset-path asset-paths]
            (let [{:keys [path hash] :as file} (get path->file asset-path)]
              (writer/copy-file! file-writer
                                 file
                                 (fingerprint/fingerprint-file-path asset-path hash)))))))))
