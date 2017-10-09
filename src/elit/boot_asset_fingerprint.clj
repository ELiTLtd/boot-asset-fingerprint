(ns elit.boot-asset-fingerprint
  (:require [boot.core :as boot]
            [clojure.java.io :as io]
            [elit.fingerprint :as fingerprint]))

(defprotocol FileUtils
  (get-files [this])
  (update-file! [this path contents])
  (copy-file! [this file dest-path]))

(defrecord BootFileUtils [fileset]
  FileUtils
  (get-files [_]
    (boot/input-files fileset))
  (update-file! [_ path contents]
    (prn "updating file text!" path "to" contents))
  (copy-file! [_ src-path dest-path]
    (prn "copying" src-path "to " dest-path)))

(defn test-fingerprint
  [fileset {:keys [asset-root asset-host extensions skip?] :as opts}]
  (let [file-writer (->BootFileUtils fileset)
        files (get-files file-writer)
        path->file (->> (map (juxt :path identity) files)
                        (into {}))]
    (loop [[{:keys [path] :as file} :as content-files] (boot/by-ext extensions files)
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
