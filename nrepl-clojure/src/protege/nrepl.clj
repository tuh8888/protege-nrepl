(ns protege.nrepl
  (:require [nrepl.server :as nrepl]
            [clojure.java.io :as io]
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

;; hook system -- identical to tawny.util -- ah well!
(defn make-hook
  "Make a hook."
  []
  (atom []))

(defn add-hook
  "Add func to hook."
  [hook func]
  (when-not
      (some #{func} @hook)
    (swap! hook conj func))
  @hook)

(defn remove-hook
  "Remove func from hook."
  [hook func]
  (swap! hook
         (partial
          remove #{func})))

(defn clear-hook
  "Empty the hook."
  [hook]
  (reset! hook []))

(defn run-hook
  "Run the hook with optional arguments. Hook functions are run in the order
that they were added."
  ([hook]
     (doseq [func @hook] (func)))
  ([hook & rest]
     (doseq [func @hook] (apply func rest))))


(def start-server-hook (make-hook))

(def servers (atom {}))


(defn start-server
  ([editorkit port]
   (binding [protege.model/*owl-editor-kit*    nil #_editorkit
             protege.model/*owl-work-space*    nil #_ (.getOWLWorkspace editorkit)
             protege.model/*owl-model-manager* nil #_ (.getOWLModelManager editorkit)]
     (run-hook start-server-hook)
     (let [server
           (nrepl/start-server :port port
             #_#_:handler @nrepl-handler)]
       (swap! servers assoc editorkit server)))))

(defn stop-server [editorkit server]
  (swap! servers dissoc editorkit)
  (nrepl/stop-server server))
