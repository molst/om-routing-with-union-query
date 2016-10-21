(ns myapp.boot-dev
  (:require [boot.core :refer :all]
            [boot.task.built-in :refer :all]
            [pandeiro.boot-http :as http]))

(deftask start
  []
  (comp
   (http/serve :dir "target/www" :port 4410)
   (target :dir #{"target"})))
