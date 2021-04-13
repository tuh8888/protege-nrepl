(ns protege-nrepl.websocket
  (:require [aleph.http :as http]
            [compojure.core :as compojure :refer [GET]]
            [compojure.route :as route]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [ring.middleware.params :as params]
            [clojure.java.data :as j]
            [cognitect.transit :as t]
            [protege-nrepl.protege-interop :as protege]
            [ring.middleware.transit :refer [encode]]
            [tawny.render :as tr])
  (:import [java.io ByteArrayOutputStream]
           [org.semanticweb.owlapi.model OWLOntologyChangeListener]))

(def non-websocket-request
  {:status  400
   :headers {"content-type" "application/text"}
   :body    "Expected a websocket request."})

(def ontology-changes (atom (s/stream 10)))

(defn protege-handler
  [req]
  (d/let-flow [conn (d/catch
                        (http/websocket-connection req)
                        (fn [_] nil))]
    (if conn
      ;; Take the first two messages, which give us the chatroom and name
      (do
        (println "Connected to:" (:remote-addr req))
        (s/connect (s/map encode @ontology-changes) conn))
      ;; if it wasn't a valid websocket handshake, return an error
      non-websocket-request)))

(def handler
  (params/wrap-params
    (compojure/routes
      (GET "/protege" [] protege-handler)
      (route/not-found "No such page."))))

(defonce server (atom nil))

(defn write-message [m]
  (with-open [out (ByteArrayOutputStream.)]
    (-> out
      (t/writer :json)
      (t/write m))
    (str out)))

(comment
  (def port 10003)
  (do (swap! server (fn [server]
                      (when server
                        (.close server))
                      (http/start-server handler {:port 10003})))

      (reset! ontology-changes (s/stream 100))

      (s/try-put! @ontology-changes {:hi "there"} 100.0))
  (s/try-put! @ontology-changes {:hi "there"} 100.0))

(def owl-changes (atom []))

(defn make-ont-listener []
  (proxy [OWLOntologyChangeListener] []
    (ontologiesChanged [changes]
      (println "Changes:" (count changes))
      (try
        (let [bean-changes (seq (j/from-java changes))]
          #_(println "Success?" @(s/try-put! @ontology-changes bean-changes 100))
          (reset! owl-changes {:bean     bean-changes
                               :changes  changes
                               :success? true}))
        (catch StackOverflowError e
          (println "Failure")
          (reset! owl-changes {:changes  changes
                               :success? false}))))))

(comment

  (count @protege/ont-listeners)
  (protege/remove-ont-listeners!)

  (protege/add-ont-listener! (make-ont-listener))

  (require '[clojure.pprint :as pp])
  (-> @owl-changes
    :changes
    (j/from-java)
    #_
    j/from-java
    #_(->>
        #_(filter #(= :SubClassOf (get-in % [:axiom :axiomType])))
        (map protege/simplify-axiom))
    #_(pp/pprint)
    #_(->> (map :axiom)
        (map :axiomType))
    #_(j/from-java-shallow {:omit #{:ontology
                                    :signature}})
    #_tr/as-form

    #_bean
    #_tr/as-form
    #_tr/as-form
    #_#_#_#_#_
    (j/from-java-shallow {:omit #{:ontology
                                  :signature}})
    :changeData
    (j/from-java-shallow {})
    :axiom
    str
    #_#_
    (j/from-java-shallow {})
    :axiomType))
