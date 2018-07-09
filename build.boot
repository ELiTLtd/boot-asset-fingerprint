(set-env!
  :source-paths #{"src"}
  :dependencies '[[org.clojure/clojure   "1.9.0"  :scope "provided"]
                  [boot/core             "2.7.2"  :scope "provided"]
                  [adzerk/bootlaces      "0.1.13" :scope "test"]
                  [metosin/boot-alt-test "0.3.2"  :scope "test"]])

(require
 '[adzerk.bootlaces :as deploy]
 '[metosin.boot-alt-test :as boot-test])

(def +version+ "2.0.0")

(deploy/bootlaces! +version+)

(task-options!
 pom {:project     'elit/boot-asset-fingerprint
      :version     +version+
      :description "Boot task to fingerprint asset references in html files."
      :url         "https://github.com/ELiTLtd/boot-asset-fingerprint"
      :scm         {:url "https://github.com/EliTLtd/boot-asset-fingerprint"}
      :license     {"MIT" "https://opensource.org/licenses/MIT"}})

(deftask testing []
  (merge-env!
   :source-paths ["test/src"]
   :resource-paths ["test/resources"])
  identity)

(deftask run-tests []
  (comp
   (testing)
   (boot-test/alt-test)))

(deftask build []
  (comp
   (deploy/build-jar)
   (install)))

(deftask push-release []
  (comp
   (deploy/build-jar)
   (#'deploy/collect-clojars-credentials)
   (push
    :tag            true
    :gpg-sign       false
    :ensure-release true
    :ensure-clean   true
    :ensure-branch  "master"
    :repo           "deploy-clojars")))
