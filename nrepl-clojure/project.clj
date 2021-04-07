(defproject ucdenver.ccp/nrepl-clojure "0.2.0-SNAPSHOT"
  :description "Launch a nrepl client inside protege"
  :license {:name         "LGPL"
            :url          "http://www.gnu.org/licenses/lgpl-3.0.txt"
            :distribution :repo}
  :scm {:url  "https://github.com/phillord/protege-nrepl.git"
        :name "git"}
  :url "https://github.com/phillord/protege-nrepl"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [nrepl/nrepl "0.8.3"]
                 #_[edu.stanford.protege/org.protege.editor.core.application
                    "5.0.0-beta-16-SNAPSHOT"]
                 [edu.stanford.protege/protege-editor-core
                  "5.5.0"]
                 [clj-commons/pomegranate "1.2.0"]
                 [cider/cider-nrepl "0.25.9"]]
  ;; this is a hack workaround to
  ;; https://github.com/technomancy/leiningen/issues/1569 which otherwise adds
  ;; nrepl tools as a test dependency in the pom (which means it doesn't get
  ;; included in protege-nrepl.
  ;; This bug has been fixed in 2.4.3
  ;;:profiles {:base {:dependencies ^:replace []}}
  )
