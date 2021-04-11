(ns protege-nrepl.websocket
  (:require [aleph.http :as http]
            [compojure.core :as compojure :refer [GET]]
            [compojure.route :as route]
            [manifold.bus :as bus]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [ring.middleware.params :as params]
            [cognitect.transit :as t]
            [protege-nrepl.protege-interop :as protege])
  (:import [java.io ByteArrayOutputStream]
           [org.semanticweb.owlapi.model OWLOntologyChangeListener]))

(def non-websocket-request
  {:status  400
   :headers {"content-type" "application/text"}
   :body    "Expected a websocket request."})

(defn echo-handler
  [req]
  (->
    (d/let-flow [socket (http/websocket-connection req)]
      (s/connect socket socket))
    (d/catch
        (fn [_]
          non-websocket-request))))

(def chatrooms (bus/event-bus))

(defn chat-handler
  [req]
  (d/let-flow [conn (d/catch
                        (http/websocket-connection req)
                        (fn [_] nil))]
    (if conn
      ;; Take the first two messages, which give us the chatroom and name
      (d/let-flow [room (s/take! conn)
                   name (s/take! conn)]
        ;; take all messages from the chatroom, and feed them to the client
        (s/connect
          (bus/subscribe chatrooms room)
          conn)
        ;; take all messages from the client, prepend the name, and publish it to the room
        (s/consume
          #(bus/publish! chatrooms room %)
          (->> conn
            (s/map #(str name ": " %))
            (s/buffer 100)))
        ;; Compojure expects some sort of HTTP response, so just give it `nil`
        nil)
      ;; if it wasn't a valid websocket handshake, return an error
      non-websocket-request)))


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
        (s/connect @ontology-changes conn))
      ;; if it wasn't a valid websocket handshake, return an error
      non-websocket-request)))

(def handler
  (params/wrap-params
    (compojure/routes
      (GET "/echo" [] echo-handler)
      (GET "/chat" [] chat-handler)
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
  (reset! server (http/start-server handler {:port port}))
  (.close @server)

  (reset! ontology-changes (s/stream 100))

  (s/try-put! @ontology-changes (write-message 100) 100))


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
