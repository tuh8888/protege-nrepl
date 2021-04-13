(ns protege-nrepl.dialog
  (:require [protege-nrepl.core :as pnrepl])
  (:import [javax.swing BoxLayout JButton JLabel JPanel JTextField]
           java.awt.BorderLayout
           java.awt.event.ActionListener))

(defn update-panel [port-val {:keys [connect disconnect status warning]}]
  (.setEnabled disconnect true)
  (.setEnabled connect false)
  (.setText status (str "Connected on port: " port-val))
  (.setText warning "OK"))

(defn start-server-action [editorkit {:keys [port warning] :as components}]
  (try
    (dosync
      (let [port (Integer/parseInt (.getText port))]
        (pnrepl/start-server editorkit port)
        (update-panel port components)))
    (catch java.net.BindException e
      (.setText warning (.getMessage e)))))

(defn stop-server-action [{:keys [connect disconnect status]}]
  (dosync
    (.setEnabled connect true)
    (.setEnabled disconnect false)
    (.setText status "Disconnected")
    (pnrepl/stop-server)))

(defn new-dialog-panel [editorkit]
  (let [pn           (JPanel.)
        ;; this one takes so a text box with next available port
        ;; and a status bar saying what, er, the status is
        middle-panel (JPanel.)
        ;; takes a set of buttons, "Connect", "Disconnect", "Close"
        ;; activated as appropriate.
        south-panel  (JPanel.)
        button-panel (JPanel.)
        port-label   (JLabel. "Port")

        components {:port       (JTextField. (str (get @pnrepl/server 7830)) 20)
                    :status     (JLabel. "Disconnected")
                    :warning    (JLabel. "OK")
                    :connect    (JButton. "Connect")
                    :disconnect (JButton. "Disconnect")}]
    (doto (:connect components)
      (.addActionListener (proxy [ActionListener] []
                            (actionPerformed [_]
                              (start-server-action editorkit components)))))
    (doto (:disconnect components)
      (.setEnabled false)
      (.addActionListener (proxy [ActionListener] []
                            (actionPerformed [_]
                              (stop-server-action
                                connect-btn disconnect-btn
                                status-label)))))

    (doto pn
      (.setLayout (BorderLayout.))
      (.add middle-panel BorderLayout/CENTER)
      (.add south-panel BorderLayout/SOUTH))
    (doto south-panel
      (.setLayout (BorderLayout.))
      (.add button-panel BorderLayout/NORTH)
      (.add (doto (JPanel.)
              (.setLayout (BorderLayout.))
              (.add (:status components) BorderLayout/NORTH)
              (.add (:warning components) BorderLayout/SOUTH))))
    (doto button-panel
      (.setLayout (BoxLayout. button-panel BoxLayout/X_AXIS))
      (.add (:connect components))
      (.add (:disconnect components)))
    (doto middle-panel
      (.setLayout (BorderLayout.))
      (.add port-label BorderLayout/WEST)
      (.add (:port components) BorderLayout/CENTER))

    (when @pnrepl/server
      (let [port (-> @pnrepl/server
                   vals
                   first
                   :port)]
        (update-panel port components)))
    pn))

(defn new-dialog [manager]
  (doto (javax.swing.JFrame.)
    (.. getContentPane (add (new-dialog-panel manager)))
    (.pack)
    (.setVisible true)))
