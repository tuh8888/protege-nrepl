(ns protege.OWLOntologyChangeListenerImpl
  (:gen-class
   :name protege.OWLOntologyChangeListenerImpl
   :main false
   :implements [OWLOntologyChangeListener]
   :methods [[ontologiesChanged [List] void]]))

(defn -ontologiesChanged [list]
  (println (bean list)))
