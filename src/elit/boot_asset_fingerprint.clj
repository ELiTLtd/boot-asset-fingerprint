(ns elit.boot-asset-fingerprint
  (:require [boot.core :as boot]
            [elit.fingerprint :refer [append-hash fingerprint]]))

(defprotocol FileUtils
  (get-files [this])
  (update-file! [this path contents])
  (copy-file! [this file dest-path]))

(defrecord BootFileUtils [fileset]
  FileWriter
  (get-files [_]
    (boot/input-files fileset))
  (update-file! [_ path contents]
    (prn "updating file text!" path))
  (copy-file! [_ src-path dest-path]
    (prn "copying" src-path "to " dest-path)))

(defn test-fingerprint
  [fileset]
  (let [file-writer (->BootFileWriter fileset)
        extensions [".html" ".css"]
        files (get-files file-writer)
        path->file (->> (map (juxt :path identity) files)
                        (into {}))]
    (loop [[file :as content-files] (boot/by-ext extensions files)
           asset-paths []]
      (if file
        (let [{:keys [file file-asset-paths]} (fingerprint file path->file {:extensions extensions})]
          (update-file! file-writer (:path file) (:file-text file))
          (recur (rest content-files)
                 (concat asset-paths file-asset-paths)))
        (doseq [asset-path asset-paths]
          (let [{:keys [path hash] :as file} (get path->file asset-path)]
            (copy-file! file-writer path (append-hash asset-path hash))))))))
