(task-options!
  pom {:project 'myapp
       :version "0.1.0-SNAPSHOT"}
  jar {:manifest {"myapp-hoho" "myapp-haha"}})

(System/setProperty "BOOT_EMIT_TARGET" "no")

(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.89"] ;Latest release working with boot-cljs, see https://github.com/adzerk-oss/boot-cljs/issues/133
                 [org.clojure/core.async "0.2.385"]
                 [org.omcljs/om "1.0.0-alpha45"]
                 [cljsjs/react-dom-server "15.2.1-1"] ; needed for sablono-server/render in arbor.cljs
                 [com.novemberain/pantomime "2.3.0"]
                 [racehub/om-bootstrap "0.6.1"]
                 [sablono "0.7.4"]
                 [domina "1.0.3"]])

(deftask local-dev "initiates local dev dependencies"
  []
  (set-env! :source-paths #(conj % "dev"))
  (set-env! :dependencies #(into % '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
                                     [adzerk/boot-reload "0.4.12" :scope "test"]
                                     [pandeiro/boot-http "0.7.3" :scope "test"]
                                     [org.clojure/tools.namespace "0.2.10" :scope "test"]]))
  (#'clojure.core/load-data-readers)
  (binding [*data-readers* (.getRawRoot #'*data-readers*)]
    (require
     '[adzerk.boot-cljs :refer :all]
     '[adzerk.boot-reload :refer :all]
     '[myapp.boot-dev :refer :all]))
  identity)
