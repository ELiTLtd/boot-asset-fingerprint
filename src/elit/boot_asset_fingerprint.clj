(ns elit.boot-asset-fingerprint
  {:boot/export-tasks true}
  (:require [boot.core :as boot]
            [clojure.java.io :as io]
            [elit.dir-writer :as writer]
            [elit.fingerprint :as fingerprint]))

(def default-extensions [".html"])

(defn asset-fingerprint*
  [files out-dir {:keys [asset-root asset-host extensions path->file skip?] :as opts}]
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

(boot/deftask asset-fingerprint
  "Replace asset references with a URL query-parameter based on the hash contents.

  The main purpose of doing this is for cache-busting static assets
  that were deployed with a far-future expiration date. See the Ruby
  on Rails Asset Pipeline guide, segment \"What is Fingerprinting and
  Why Should I Care\" for a detailed explanation of why you want to do this.
  (http://guides.rubyonrails.org/asset_pipeline.html#what-is-fingerprinting-and-why-should-i-care-questionmark) "
  [a asset-root        ROOT  str     "The root dir where the assets are served from"
   e extensions        EXT   [str]   "Add a file extension to indicate the files to process for asset references"
   r regexes           RES   [regex] "A list of regexes for filtering file paths"
   o asset-host        HOST  str     "Host to prefix all asset urls with e.g. https://your-host.com"
   s skip                    bool    "Skips file fingerprinting and replaces each asset reference with the unfingerprinted path"
   t strict                  bool    "Throws an exception if an asset is not found in the fileset. Defaults to true"]
  (let [out-dir (boot/tmp-dir!)]
    (boot/with-pre-wrap fileset
      (let [files (boot/input-files fileset)]
        (asset-fingerprint* (-> (concat
                                 (boot/by-ext (or extensions
                                                  default-extensions)
                                              files)
                                 (boot/by-re regexes files))
                                (distinct))
                            out-dir
                            {:asset-host asset-host
                             :asset-root asset-root
                             :path->file (->> (map (juxt :path identity) files)
                                              (into {}))
                             :skip? skip
                             :strict? (or strict true)})
        (-> fileset
            (boot/add-resource out-dir)
            (boot/commit!))))))
