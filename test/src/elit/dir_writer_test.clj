(ns elit.dir-writer-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [elit.dir-writer :as sut])
  (:import [elit.dir_writer TmpDirWriter]
           [java.util UUID]))

(defn tmp-dir
  []
  (str "/tmp/" (UUID/randomUUID)))

(deftest updating-files-in-output-directory
  (let [out-dir (tmp-dir)
        path "foo/bar"
        contents "123"
        writer (sut/->TmpDirWriter out-dir)]
    (try
      (sut/update-file! writer path contents)
      (let [out-file (io/file out-dir path)]
        (testing "output file exists"
          (is (.exists out-file)))
        (testing "output file content has been written"
          (is (= contents (slurp (str out-dir "/" path))))))
      (finally
        (doto (io/file out-dir path)
          (io/delete-file))))))

(deftest copying-files-into-output-directory
  (let [in-dir (tmp-dir)
        out-dir (tmp-dir)
        src-path "foo/bar"
        dst-path "bar/baz"
        file {:dir in-dir :path src-path}
        contents "123"
        writer (sut/->TmpDirWriter out-dir)]
    (try
      (io/make-parents (io/file in-dir src-path))
      (spit (str in-dir "/" src-path) contents)
      (testing "file is copied from file (src) dir if not found in out-dir"
        (sut/copy-file! writer file dst-path)
        (is (= contents
               (slurp (str out-dir "/" dst-path)))))
      (let [out-file-contents "456"]
        (io/make-parents (io/file out-dir src-path))
        (spit (str out-dir "/" src-path) out-file-contents)
        (testing "file with matching path is copied from out-dir if it exists"
          (sut/copy-file! writer file dst-path)
          (is (= "456"
                 (slurp (str out-dir "/" dst-path))))))
      (finally
        (doseq [[dir path] [[in-dir src-path]
                            [out-dir src-path]
                            [out-dir dst-path]]]
          (doto (io/file dir path)
            (io/delete-file)))))))
