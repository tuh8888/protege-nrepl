(ns protege.dialog
  (:require [aleph.http :as http]
            [protege.model :as protege]
            [protege.nrepl :as protege-nrepl]
            [protege.websocket :as websocket])
  (:import java.awt.BorderLayout
           java.awt.event.ActionListener
           [org.semanticweb.owlapi.model OWLOntologyChangeListener]
           [javax.swing BoxLayout JButton JLabel JPanel JTextField]))

(def last-port (ref 7830))
;; map between model manager and port
(def servers (ref {}))

(defn start-server-action [editorkit connect-btn disconnect-btn port-field status-label warning-label]
  (try
    (dosync
      (let [port (Integer/parseInt (.getText port-field))
            s    (protege-nrepl/start-server
                   editorkit port)]
        (alter servers merge {editorkit s})
        (.setEnabled disconnect-btn true)
        (.setEnabled connect-btn false)
        (.setText status-label
          (str "Connected on port: " port))
        (.setText warning-label "OK")
        (ref-set last-port port)))
    (catch java.net.BindException e
      (.setText warning-label (.getMessage e)))))

(defn stop-server-action [editorkit connect disconnect status]
  (dosync
    (let [s (get @servers editorkit)]
      (alter servers dissoc editorkit)
      (.setEnabled connect true)
      (.setEnabled disconnect false)
      (.setText status "Disconnected")
      (protege.nrepl/stop-server editorkit s))))

(defn new-dialog-panel [editorkit]
  (let [pn             (JPanel.)
        ;; this one takes so a text box with next available port
        ;; and a status bar saying what, er, the status is
        middle-panel   (JPanel.)
        ;; takes a set of buttons, "Connect", "Disconnect", "Close"
        ;; activated as appropriate.
        south-panel    (JPanel.)
        button-panel   (JPanel.)
        port-label     (JLabel. "Port")
        port-field     (JTextField. (str @last-port) 20)
        status-label   (JLabel. "Disconnected")
        warning-label  (JLabel. "OK")
        connect-btn    (JButton. "Connect")
        disconnect-btn (JButton. "Disconnect")]
    (doto connect-btn
      (.addActionListener (proxy [ActionListener] []
                            (actionPerformed [_]
                              (start-server-action
                                editorkit
                                connect-btn disconnect-btn
                                port-field status-label
                                warning-label)))))
    (doto disconnect-btn
      (.setEnabled false)
      (.addActionListener (proxy [ActionListener] []
                            (actionPerformed [_]
                              (stop-server-action
                                editorkit connect-btn disconnect-btn
                                status-label)))))

    #_(when @protege/auto-connect-on-default
        (connect-fn nil))

    (doto pn
      (.setLayout (BorderLayout.))
      (.add middle-panel BorderLayout/CENTER)
      (.add south-panel BorderLayout/SOUTH))
    (doto south-panel
      (.setLayout (BorderLayout.))
      (.add button-panel BorderLayout/NORTH)
      (.add (doto (JPanel.)
              (.setLayout (BorderLayout.))
              (.add status-label BorderLayout/NORTH)
              (.add warning-label BorderLayout/SOUTH))))
    (doto button-panel
      (.setLayout (BoxLayout. button-panel BoxLayout/X_AXIS))
      (.add connect-btn)
      (.add disconnect-btn))
    (doto middle-panel
      (.setLayout (BorderLayout.))
      (.add port-label BorderLayout/WEST)
      (.add port-field BorderLayout/CENTER))
    pn))

(defonce server (atom nil))

(defn new-dialog [manager]
  (doto (javax.swing.JFrame.)
    (.. getContentPane (add (new-dialog-panel manager)))
    (.pack)
    (.setVisible true)))

(comment
  (reset! server (http/start-server websocket/handler {:port 10002}))


  (-> protege.model/*owl-model-manager*
    bean
    keys)
  (def owl-changes (atom []))
  (.addOntologyChangeListener protege.model/*owl-model-manager*
    (proxy [OWLOntologyChangeListener] []
      (ontologiesChanged [changes]
        (println "bye")
        (reset! owl-changes (map bean changes)))))

  (->> @owl-changes
    first
    vals
    (map type)))
