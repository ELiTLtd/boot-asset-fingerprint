(ns elit.fingerprint-test
  (:require [clojure.test :refer [deftest is testing]]
            [elit.fingerprint :as sut]))

(deftest detecting-asset-paths
  (testing "empty reference is ignored"
    (is (empty? (sut/find-asset-refs "foo ${} bar" {}))))
  (testing "non-empty reference is detected"
    (is (= ["baz"] (sut/find-asset-refs "foo ${baz} bar" {}))))
  (testing "leading slash is stripped"
    (is (= ["baz"] (sut/find-asset-refs "foo ${/baz} bar" {}))))
  (testing "asset root is added"
    (is (= ["public/baz"] (sut/find-asset-refs "foo ${/baz} bar" {:asset-root "public"})))))

(deftest fingerprinting-file-paths
  (testing "hash is appended to filename before the suffix"
    (is (= (sut/fingerprint-file-path "path/to/file.ext" "123")
           "path/to/file-123.ext"))))

(deftest replacing-references-to-assets
  (let [rel-path "bar/baz.ext"
        abs-path (str "foo/" rel-path)
        hash 123]
    (testing "pathed reference to asset is replaced"
      (is (= (sut/replacer abs-path {:path->file {abs-path {:hash hash}}})
             (sut/fingerprint-file-path abs-path hash))))
    (testing "no-op if skip? is true"
      (is (= (sut/replacer abs-path {:path->file {abs-path {:hash hash}}
                                     :skip? true})
             abs-path)))
    (testing "asset host is prepended if provided"
      (is (= (sut/replacer abs-path {:asset-host "https://my.cdn/assets/"
                                     :path->file {abs-path {:hash hash}}})
             "https://my.cdn/assets/foo/bar/baz-123.ext")))
    (testing "asset root is applied correctly"
      (is (= (sut/replacer rel-path {:asset-root "foo"
                                     :path->file {abs-path {:hash hash}}
                                     :strict? true})
             "bar/baz-123.ext")))
    (testing "options work together"
      (is (= (sut/replacer rel-path {:asset-host "https://my.cdn/assets"
                                     :asset-root "foo"
                                     :path->file {abs-path {:hash hash}}
                                     :strict? true})
             "https://my.cdn/assets/bar/baz-123.ext")))
    (testing "throws exception if strict? option is set and file not found"
      (is (try (sut/replacer rel-path {:path->file {} :strict? true})
               (catch Exception _ true))))))
