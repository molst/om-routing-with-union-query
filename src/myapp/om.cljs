(ns myapp.om "om helper functions"
  (:require [om.next :as om :refer-macros [defui]]
            [om.next.impl.parser :as parser]
            [om.util :as omutil]
            [cljs.pprint :refer [pprint]]))

;; Found here:  https://github.com/awkay/om-tutorial/blob/master/src/main/om_tutorial/parsing.cljs
(defn descend
  "Descend the state database, tracking position in :db-path. If given a ref as the key, rewrites
   db-path to be rooted at that ref."
  [env key]
  (if (omutil/ident? key)
    (assoc env :db-path [key])
    (update-in env [:db-path] conj key)))

(defn elide-empty-query
  "Helper method to prevent a remote request parse for the current key if the sub-parser response is empty.
  `target`: Remote target name
  `response`: The response from a recursive call to the parser
  Emits {target response} if the response is non-empty; otherwise nil. Used by `recurse-remote`."
  [target {:keys [query] :as response}]
  (if (or (nil? query) (empty? query))
    (do (println "empty query after recurse, no luck this time...")
        nil)
    (do (println "ayee!! ")
        (pprint {target response})
      {target response})))

(defn recurse-remote
  "Recursively calls the parser on the subquery (which will completely determine the result of
  whether or not a remote request is needed). Call this when you want to delegate the remote
  response decision to the sub-query, and use the return value of this function as the proper
  return value of read. Returns {target query} if there should be a request, or nil).
  Basically, use this on any client-local keyword that needs to be skipped over to get to the
  'real' server query."
  [{:keys [target ast parser] :as env} key descend?]
  (let [env' (if descend? (descend env key) env)]
    (if (:target ast) "FORCED REMOTE READ")
    #_(when target
      (println "recursing remote target: " target ", query: ")
      (pprint (:query ast)))
    (when target
      (elide-empty-query target (update-in ast [:query] #(let [_ (println "recursing with query: " %)
                                                               v (parser env' % target)] (println "remrecurse val:" v) v))))))

;; Found here: https://github.com/compassus/compassus/issues/12
(defn flattening-parser
  "Takes a parser. Returns a parser which ignores the root keys of the queries
  it's given, and reads the keys joined to those keys as if they were root keys,
  using the given parser.

  This is particularly useful when using Compassus. Compassus has its own parser
  which calls your parser. The root query it passes to your parser has a single
  key, which is the current route, which is joined to that route component's
  query. That is, if the current route is :app/home and that route matches the
  following component:

  (defui Home
    static IQuery
    (query [this]
      [{:some/data [:some/property]}]))

  then Compassus will ask your parser to read the query:

  [{:app/home [{:some/data [:some/property]}]}]

  If you don't care about the route key, and you'd like your parser to
  treat :some/data as a root-level key in the query, wrap your parser in
  flattening-parser. Your parser will instead read the query:

  [{:some/data [:some/property]}]"
  [parser]
  (om/parser
   {:read (fn [{:keys [query target] :as env} key params]
            (if-not target
              {:value (parser env query)}
              {target (let [subquery (parser env query target)]
                        (when (seq subquery)
                          (parser/expr->ast {key subquery})))}))
    :mutate (fn [{:keys [ast target] :as env} key params]
              (let [tx [(om/ast->query ast)]]
                (if-not target
                  (let [{:keys [result ::om/error] :as ret}
                        (get (parser env tx) key)]
                    {:value (dissoc ret :result ::om/error)
                     :action #(or result (throw error))})
                  (let [[ret] (parser env tx target)]
                    {target (cond-> ret
                              (some? ret) parser/expr->ast)}))))}))
