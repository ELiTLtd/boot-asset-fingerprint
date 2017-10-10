(ns elit.boot-asset-fingerprint-test
  (:require [boot.core :as boot]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [deftest is testing]]
            [elit.boot-asset-fingerprint :as sut]
            [elit.fingerprint :as fingerprint]))

(defn asset-refs
  [text asset-host]
  (->> (re-seq (re-pattern (str asset-host ".+\\.(css|js)"))
               text)
       (map first)))

(defn remove-asset-host
  [asset-ref asset-host]
  (string/replace-first asset-ref (re-pattern asset-host) ""))

(deftest boot-task-fingerprints-assets
  (let [asset-root "public"
        asset-host "https://my.cdn/assets"
        text (slurp (io/resource "public/index.html"))
        refs (asset-refs text asset-host)]
    (boot/boot
     (sut/asset-fingerprint :asset-root asset-root
                            :asset-host asset-host))
    (testing "no trace of ${} in the text"
      (is (nil? (re-find fingerprint/asset-regex text))))
    (testing "asset root has been removed from refs"
      (is (every? #(not ( string/includes? % asset-root)) refs)))
    (testing "fingerprinted files exist on the resource path"
      (let [public-files (->> (file-seq (io/file (io/resource "public")))
                              (map #(.getPath %)))]
        (doseq [asset-ref refs]
          (is (some #(string/ends-with? % (remove-asset-host asset-ref asset-host))
                    public-files)
              asset-ref))))))
