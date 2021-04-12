#!/usr/bin/env bb
(ns build
  (:require [clojure.java.shell :refer [sh with-sh-dir with-sh-env]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [babashka.fs :as fs]))

(def cli-options
  [["-c" "--component COMPONENT" "Only build server or plugin"
    :default nil
    :parse-fn keyword
    :validate [#{:server :plugin} "Must be one of 'server' or 'plugin'"]
    :id :component]
   ["-g" "--gui" "Run protege after"
    :default false
    :id :protege?]
   ["-h" "--help"]])

(defn wrap-str
  [& args]
  (str (apply str "\"" args) "\""))

(defn jar-file-name
  [& name-components]
  (io/file "target"
    (str
      (->> name-components
        (interpose "-")
        (apply str))
      ".jar")))

(defmacro execute [& body]
  `(let [{out#  :out
          exit# :exit
          err#  :err} (do ~@body)
         out#         (str/trim out#)
         err#         (str/trim err#)]
     (when-not (empty? out#) (println out#))
     (if (zero? exit#)
       true
       (println err#))))

(defn install-server [{:keys [jar version]}]
  (println "Installing server jar" (str jar))
  (execute
    (sh "clj" "-X:depstar" "uberjar"
      ":version" (wrap-str version)
      ":jar" (wrap-str jar))))

(defn deploy-server-jar [{:keys [jar pom]}]
  (println "Deploying server jar" (str jar) "locally")
  (execute
    (sh "clj" "-X:deps" "mvn-install"
      ":jar" (wrap-str jar)
      ":pom" (wrap-str pom))))

(defn build-server [{:keys [project version] :as args}]
  (println "Building server" project version)
  (with-sh-dir "server"
    (let [args (-> args
                 (assoc
                   :jar (jar-file-name project "server" version)
                   :pom "pom.xml"))]

      (execute (sh "rm" (:pom args)))

      (and
        (install-server args)
        (deploy-server-jar args)))))

(defn clean-protege-plugins [{:keys [protege]}]
  (println "Removing existing protege plugins" (str protege))
  (->> "protege-nrepl*.jar"
    (fs/glob (io/file "/" "opt" "protege" "plugins"))
    (map fs/delete)))

(defn install-plugin-jar [{:keys [version]}]
  (println "Installing plugin jar")
  (with-sh-env (-> (System/getenv)
                 (->> (into {}))
                 (assoc :RELEASE_VERSION version))
    (execute (sh "mvn" "-DskipTests=true" "install"))))

(defn deploy-plugin-jar [{:keys [project version protege]}]
  (println "Deploying plugin jar")
  (let [jar (jar-file-name project version)]
    (execute (sh "cp" (str jar) (str protege)))))

(defn build-plugin [{:keys [project version] :as args}]
  (println "Building plugin" project version)
  (clean-protege-plugins args)

  (with-sh-dir "plugin"
    (and
      (install-plugin-jar args)
      (deploy-plugin-jar args))))

(let [{{:keys [component protege? help]}
       :options
       :as cmd} (parse-opts *command-line-args* cli-options)
      args      {:version "0.2.0"
                 :project "protege-nrepl"
                 :protege (io/file "/" "opt" "protege" "plugins")}]
  (or (and help
        (do (println (:summary cmd))
            true))
    (and
      (or (= component :plugin)
        (build-server args))
      (or (= component :server)
        (build-plugin args))
      (or (not protege?)
        (execute (sh "protege")))
      (println "Success"))))
