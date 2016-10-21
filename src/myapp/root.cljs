(ns myapp.root
  (:require [cljs.core.async :refer [chan put! <!]]
            [om.next :as om :refer-macros [defui]]
            [om.next.impl.parser :as parser]
            [om.util :as omutil]
            [om.dom :as dom]
            [sablono.core :as sabl :refer-macros [html]]
            [cljs.pprint :refer [pprint]]
            [myapp.om-primitives :as opr]
            [myapp.om :as oh]))

(enable-console-print!)

(defmethod opr/read :a-component [{:keys [state parser query ast target] :as env} k params]
  (println "read bed, target: " target)
  (let [st @state]
    {:value (om/db->tree query st st)}))

(defmethod opr/read :default [{:keys [state parser query ast target] :as env} k params]
  (println "default read (" k ", query: " query ", target: " target ") ast: ")
  (pprint ast)
  ;the intention of the default reader is just to recurse
  (let [st @state]
    (if target ;remote parsing mode
      (let [rec-result (oh/recurse-remote env k true)]
        (println "default recurse result: ")
        (pprint rec-result)
        rec-result)
      {:value (parser env query)})))

(def parser (om/parser {:read opr/read :mutate opr/mutate}))

(defn put-remote-queries [c]
  (fn [{:keys [myremote]} cb]
    (println "remote send!!")
    (when myremote
      (println "myremote send!!!!")
      (pprint myremote)
      (let [{[{[{[myremote] :children}] :children}] :children :as ast} (om/query->ast myremote)
            myremote-params (get-in myremote [:params])]
        (println "send ast:")
        (pprint ast)
        (put! c [myremote-params cb])))))

(defui AComponent
  static om/IQuery
  (query [this] '[[:a-component-content _]])
  Object
  (render [this]
    (let [{:keys [a-component-content]} (om/props this)]
      (html
       [:div
        (str "a-component-content: " a-component-content)]))))

(def a-component (om/factory AComponent))

(defui AView
  static om/IQuery
  (query [this] [{:a-component (om/get-query AComponent)}])
  Object
  (render [this]
    (let [props (om/props this)
          q (om/get-query this)]
      (dom/div nil (a-component (:a-component props))))))

(def route->component
  {:a-view AView})

(def route->factory
  (zipmap (keys route->component)
          (map om/factory (vals route->component))))

(defmethod opr/read :route/data
  [{:keys [parser state query ast target] :as env} k params]
  #_(println ":route/data, key: " k)
  (let [st @state
        route (get st :app/route)
        route-path (cond-> route
                     (= (second route) '_) pop)
        route-key (first route-path)
        route-query (route-key query)

        ;;pick the route branch of the ast, which is a union entry
        route-ast (first (filter #(= route-key (:union-key %)) (-> ast :children first :children)))

        ;;make it a join instead of a union entry
        route-ast-join (-> route-ast
                           (dissoc :union-key)
                           (assoc :dispatch-key route-key :key route-key :type :join))

        ;;make use of the hacked ast in the env
        route-env (assoc env :ast route-ast-join :query route-query)]

     ;; This is a working alternative to the recursion below. Not sure why the db tree is delivered in a vector though.
     #_{:value (first (om/db->tree (route-key query) route-path st))}

     ;; This kind-of works, but not with remotes, because we need to use something like oh/recurse-remote
     #_(let [st @state]
       (if (leaf-query? ast)
         (opr/read-leaf env k params)
         {:value (parser env (route-key query))}))

     ;; This uses the hacked ast (see above). Works for the first read, but subsequent reads are broken.
     #_(if target
       (let [_ (println "route/data before recurse (" k ", " route-key "), ast: ")
             _ (pprint ast)
             rec-result (oh/recurse-remote route-env route-key true)]
           (println "route/data recurse result: ")
           (pprint rec-result)
           rec-result)
       {:value (parser env route-query)})

     ;; Trying to use resolve the union query as done in the om test case https://github.com/omcljs/om/blob/master/src/test/om/next/tests.cljc#L1311
     (if target
       (let [_ (println "route/data before recurse (" k ", " route-key "), ast: ")
             _ (pprint ast)
             rec-result (oh/recurse-remote env route-key true)]
           (println "route/data recurse result: ")
           (pprint rec-result)
           rec-result)
       {:value (parser env route-query)})))

(defmethod opr/read :app/route2
  [{:keys [parser state query ast target] :as env} k params]
  (println "route 2...")
  #_(pprint query)
  (if target
       (let [rec-result (oh/recurse-remote env k true)]
           (println "route/data recurse result: ")
           (pprint rec-result)
           rec-result)
       {:value (parser env query)}))

(defmethod opr/read :app/route
  [{:keys [state query]} k _]
  (let [st @state] {:value (get st k)}))

(defmethod opr/mutate 'change/route! [{:keys [state]} _ {:keys [route]}]
  {:value {:keys [:app/route]}
   :action #(swap! state assoc :app/route route)})

(defui RootView
  static om/IQuery
  (query [this]
    [{:app/route2
      {:a-view (om/get-query AView)}}]
    #_[:app/route
     {:route/data
      (zipmap (keys route->component)
              (map om/get-query (vals route->component)))}])
  Object
  (render [this]
          #_(let [{:keys [app/route route/data] :as props} (om/props this)
                  route-key (first route)
                  route-query (route-key data)]
              (dom/div nil ((route->factory route-key) data)))
          (let [props (om/props this)]
            (dom/div nil (str "root view props: " props)))))

(def reconciler
  (om/reconciler
   {:parser parser
    :send (put-remote-queries opr/send-remote-query-chan)
    :remotes [:myremote]
    :state
    (atom
     {:app/properties {:title "My app"}
      :app/route '[:a-view _]
      :app/route2 '[:a-view]
      :a-component-content "some content"})}))

(om/add-root! reconciler RootView (js/document.getElementById "om-application-root"))

