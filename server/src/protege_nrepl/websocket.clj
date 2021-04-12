(ns protege-nrepl.websocket
  (:require [aleph.http :as http]
            [compojure.core :as compojure :refer [GET]]
            [compojure.route :as route]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [ring.middleware.params :as params]
            [cognitect.transit :as t]
            [protege-nrepl.protege-interop :as protege]
            [ring.middleware.transit :refer [encode]])
  (:import [java.io ByteArrayOutputStream]
           #_[org.semanticweb.owlapi.model OWLOntologyChangeListener]))

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
                      (.close server)
                      (http/start-server handler {:port port})))

      (reset! ontology-changes (s/stream 100))

      (s/try-put! @ontology-changes {:hi "there"} 100.0)))


(comment
  (def owl-changes (atom []))

  (defn make-ont-listener []
    (proxy [OWLOntologyChangeListener] []
      (ontologiesChanged [changes]
        (let [bean-changes (map bean changes)]
          (println "Success?" @(s/try-put! @ontology-changes "hello" 100))
          (reset! owl-changes bean-changes)))))

  (protege/add-ont-listener! (make-ont-listener))

  (count @protege/ont-listeners)
  (protege/remove-ont-listeners!)

  (->> @owl-changes
    first
    keys
    #_vals
    #_(map type)))
