(ns protege.nrepl
  (:require [clojure.java.io :as io]
            [nrepl.server :as nrepl]
            [protege.model :as protege]))

#_(def nrepl-handler (ref nrepl-server/default-handler))

;; borrowed from lein
(defn getenv
  "Wrap System/getenv for testing purposes."
  [name]
  (System/getenv name))

(defn protege-nrepl-home
  "Return full path to the user's protege home directory."
  []
  (let [protege-nrepl-home (getenv "PROTEGE_NREPL_HOME")
        protege-nrepl-home (or (and protege-nrepl-home (clojure.java.io/file protege-nrepl-home))
                             (clojure.java.io/file (System/getProperty "user.home") ".protege-nrepl"))]
    (.getAbsolutePath (doto protege-nrepl-home .mkdirs))))

(def init
  "Load the user's ~/.protege-nrepl/init.clj file, if present."
  (memoize (fn []
             (let [init-file (clojure.java.io/file (protege-nrepl-home) "init.clj")]
               (when (.exists init-file)
                 (try (load-file (.getAbsolutePath init-file))
                      (catch Exception e
                        (.printStackTrace e))))))))

(def servers (atom {}))

(defn start-server
  ([editorkit port]
   (binding [protege.model/*owl-editor-kit*    editorkit
             protege.model/*owl-work-space*    (when editorkit (.getOWLWorkspace editorkit))
             protege.model/*owl-model-manager* (when editorkit (.getOWLModelManager editorkit))]
     (let [server
           (nrepl/start-server
             :port port)]
       (swap! servers assoc editorkit server)))))

(defn stop-server [editorkit server]
  (swap! servers dissoc editorkit)
  (nrepl/stop-server server))
