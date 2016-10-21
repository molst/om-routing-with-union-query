(ns myapp.om-primitives
  (:require [cljs.core.async :refer [chan]]))

(def send-remote-query-chan (chan))

(defmulti read   (fn [_ key _] key))
(defmulti mutate (fn [_ key _] key))
