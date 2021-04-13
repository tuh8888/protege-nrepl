(ns protege-nrepl.core
  (:require [clojure.java.io :as io]
            [nrepl.server :as nrepl]
            [protege-nrepl.protege-interop :as protege]))

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
        protege-nrepl-home (or (and protege-nrepl-home (io/file protege-nrepl-home))
                             (io/file (System/getProperty "user.home") ".protege-nrepl"))]
    (.getAbsolutePath (doto protege-nrepl-home .mkdirs))))

(def init
  "Load the user's ~/.protege-nrepl/init.clj file, if present."
  (memoize (fn []
             (let [init-file (io/file (protege-nrepl-home) "init.clj")]
               (when (.exists init-file)
                 (try (load-file (.getAbsolutePath init-file))
                      (catch Exception e
                        (.printStackTrace e))))))))

(def server (ref nil))

;; map between model manager and port

(defn start-server
  ([editorkit port]
   (dosync
     (binding [protege/*owl-editor-kit*    editorkit
               protege/*owl-work-space*    (when editorkit (.getOWLWorkspace editorkit))
               protege/*owl-model-manager* (when editorkit (.getOWLModelManager editorkit))]
       (let [s (nrepl/start-server :port port)]
         (ref-set server s))))))

(defn stop-server []
  (nrepl/stop-server @server)
  (ref-set server nil))
