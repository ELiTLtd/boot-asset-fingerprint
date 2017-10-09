(ns elit.fingerprint-test
  (:require [clojure.test :refer [deftest is testing]]
            [elit.fingerprint :as sut]))

(deftest detecting-asset-paths
  (testing "empty reference is ignored"
    (is (empty? (sut/find-asset-refs "foo ${} bar"))))
  (testing "non-empty reference is detected"
    (is (= ["baz"] (sut/find-asset-refs "foo ${baz} bar")))))

(deftest fingerprinting-file-paths
  (testing "hash is appended to filename before the suffix"
    (is (= (sut/fingerprint-file-path "path/to/file.ext" "123")
           "path/to/file-123.ext"))))

(deftest replacing-references-to-assets
  (let [match "/foo/bar.ext"
        hash 123]
    (testing "matched reference to asset is replaced"
      (let [replacer (sut/replacer-fn {:path->file {match {:hash hash}}})]
        (is (= (replacer match)
               (sut/fingerprint-file-path match hash)))))
    (testing "no-op if skip? is true"
      (let [replacer (sut/replacer-fn {:path->file {match {:hash hash}}
                                       :skip? true})]
        (is (= (replacer match)
               match))))
    (testing "asset root is removed if provided"
      (let [replacer (sut/replacer-fn {:asset-root "foo"
                                       :path->file {match {:hash hash}}})]
        (is (= (replacer match)
               "/bar-123.ext"))))
    (testing "asset host is prepended if provided"
      (let [replacer (sut/replacer-fn {:asset-host "https://my.cdn/assets/"
                                       :path->file {match {:hash hash}}})]
        (is (= (replacer match)
               "https://my.cdn/assets/foo/bar-123.ext"))))
    (testing "options work together"
      (let [replacer (sut/replacer-fn {:asset-host "https://my.cdn/assets"
                                       :asset-root "foo"
                                       :path->file {match {:hash hash}}})]
        (is (= (replacer match)
               "https://my.cdn/assets/bar-123.ext"))))))
