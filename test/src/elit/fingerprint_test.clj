(ns elit.fingerprint-test
  (:require [clojure.test :refer [deftest is testing]]
            [elit.fingerprint :as sut]))

(deftest detecting-asset-paths
  (testing "empty reference is ignored"
    (is (empty? (sut/find-asset-paths "foo ${} bar"))))
  (testing "non-empty reference is detected"
    (is (= ["baz"] (sut/find-asset-paths "foo ${baz} bar")))))

(deftest fingerprinting-file-paths
  (testing "hash is appended to filename before the suffix"
    (is (= (sut/fingerprint-file-path "path/to/file.ext" "123")
           "path/to/file-123.ext"))))

(deftest replacing-references-to-assets
  (let [match "/foo/bar.ext"
        hash 123]
    (testing "matched reference to asset is replaced"
      (let [replacer (sut/replacer-fn {:path->file {match {:hash hash}}})]
        (is (= (replacer [:ignored match])
               (sut/fingerprint-file-path match hash)))))
    (testing "no-op if skip? is true"
      (let [replacer (sut/replacer-fn {:path->file {match {:hash hash}}
                                       :skip? true})]
        (is (= (replacer [:ignored match])
               match))))
    ;; TODO: asset-root
    ;; TODO: not skip and asset-host
    ;; TODO: throw exceptions
    ))
