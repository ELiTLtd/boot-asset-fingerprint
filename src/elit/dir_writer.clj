(ns elit.dir-writer
  (:require [clojure.java.io :as io]))

(defprotocol DirWriter
  (update-file! [this path contents])
  (copy-file! [this file dest-path]))

(defrecord TmpDirWriter [out-dir]
  DirWriter
  (update-file! [_ path contents]
    (prn "update-file " path contents)
    (let [out-file (io/file out-dir path)]
      (io/make-parents out-file)
      (spit out-file contents)))
  (copy-file! [_ {:keys [dir path] :as src-file} dst-path]
    (let [in-file (let [out-dir-file (io/file out-dir path)]
                    (if (.exists out-dir-file)
                      out-dir-file
                      (io/file dir path)))
          out-file (io/file out-dir dst-path)]
      (io/make-parents out-file)
      (prn "copying " in-file "to" out-file)
      (io/copy in-file out-file))))
