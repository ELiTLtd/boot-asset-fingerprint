# boot-asset-fingerprint [![Build Status](https://travis-ci.org/ELiTLtd/boot-asset-fingerprint.svg?branch=master)](https://travis-ci.org/ELiTLtd/boot-asset-fingerprint)

[![Clojars Project](https://img.shields.io/clojars/v/elit/boot-asset-fingerprint.svg)](https://clojars.org/elit/boot-asset-fingerprint)

A boot task to fingerprint your assets in order to prevent the browser from caching stale assets.

For a detailed explanation of why fingerprinting assets is a good idea, see the [Ruby on Rails asset pipeline documentation](http://guides.rubyonrails.org/asset_pipeline.html#what-is-fingerprinting-and-why-should-i-care-questionmark). The [Google web fundamentals on HTTP caching](https://developers.google.com/web/fundamentals/performance/optimizing-content-efficiency/http-caching#invalidating-and-updating-cached-responses) is also well worth a read.

Essentially, the `asset-fingerprint` task does 2 things:

* Replaces all asset references in your content files, such as html or css, wrapped in a `${}`
* Copies any referenced assets to their fingerprinted location

For example:

``` html
<script src="${js/app.js}" type="text/javascript"></script>
```
becomes
``` html
<script src="js/app-0bafd97f3bdd0dcb44ca0c6ea7d106be.js" type="text/javascript"></script>
```

with `js/app-0bafd97f3bdd0dcb44ca0c6ea7d106be.js` also committed to the fileset.

All asset reference paths are *relative to the root directory where the assets are served*. See [asset root](#asset-root).

## Typical usage

Add the `asset-fingerprint` task to your release pipeline:

``` clojure
(require '[elit.boot-asset-fingerprint :refer [asset-fingerprint]])

(deftask release []
  (comp (build-jar)
        (asset-fingerprint :extensions [".html" ".css"])))
```
It's possible to choose the files `asset-fingerprint` looks at by passing in a list of extensions. Run `boot asset-fingerprint -h` for the full list of supported options.

To serve up assets in development, call the `asset-fingerprint` task with `skip` true:

``` clojure
(deftask dev []
  (comp (watch)
        (asset-fingerprint :skip true)))
```

### Asset root
Specify the `asset-root` option if your assets are being served out of a subdirectory (such as `public`). This ensures all of the references to your assets are found in the fileset and updated correctly.
### Host assets on a CDN
Asset references can be prefixed with a base URL so that they are ready to be served up by a CDN. You can specify the base URL in the `asset-host` option.
## Attribution

Credit goes to Adam Frey for creating the original [boot-asset-fingerprint](https://github.com/AdamFrey/boot-asset-fingerprint) library.

## Copyright and License

Copyright © 2017 English Language iTutoring Limited.

Licensed under the MIT License (see the LICENSE file).
