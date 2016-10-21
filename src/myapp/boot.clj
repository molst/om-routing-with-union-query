(ns myapp.boot
  (:require [boot.core :refer :all]
            [boot.task.built-in :refer :all]
            [adzerk.boot-cljs :as cljs]
            [clojure.test :as t]))

(deftask build
  [] (comp (pom) (jar)))


