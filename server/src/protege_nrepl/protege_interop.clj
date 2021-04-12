;; The contents of this file are subject to the LGPL License, Version 3.0.
;;
;; Copyright (C) 2013, Phillip Lord, Newcastle University
;;
;; This program is free software: you can redistribute it and/or modify it
;; under the terms of the GNU Lesser General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or (at your
;; option) any later version.
;;
;; This program is distributed in the hope that it will be useful, but WITHOUT
;; ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
;; FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
;; for more details.
;;
;; You should have received a copy of the GNU Lesser General Public License
;; along with this program. If not, see http://www.gnu.org/licenses/.

(ns protege-nrepl.protege-interop
  (:require [clojure.java.data :as j]))

(def ^{:dynamic true
       :doc     "The OWLModelManager for the Protege Instance from which the REPL is launched."}
  *owl-model-manager* nil)

(def ^{:dynamic true
       :doc     "The OWLEditorKit for the Protege Instance from which the REPL is launched."}
  *owl-editor-kit* nil)

(def ^{:dynamic true
       :doc     "The OWLWorkspace for the Protege Instance from which the REPL is launched."}
  *owl-work-space* nil)

(def auto-connect-on-default (ref false))

(defn active-ontology
  ([]
   (.getActiveOntology *owl-model-manager*))
  ([o]
   (.setActiveOntology *owl-model-manager* o)))

(defn selected-object
  ([workspace]
   (-> workspace
     .getOWLSelectionModel
     .getSelectedObject))
  ([workspace entity]
   (-> workspace
     .getOWLSelectionModel
     (.setSelectedObject entity))))

(def ont-listeners (atom nil))

(defn add-ont-listener! [listener]
  (swap! ont-listeners (fn [listeners]
                         (.addOntologyChangeListener
                           *owl-model-manager*
                           listener)
                         (conj listeners listener))))

(defn remove-ont-listeners! []
  (doseq [listener @ont-listeners]
    (.removeOntologyChangeListener *owl-model-manager* listener))
  (reset! ont-listeners nil))

(defn map-vals [f m]
  (->> m
    (map (juxt key (comp f val)))
    (into {})))

(defmethod j/from-java org.semanticweb.owlapi.model.IRI
  [instance]
  (-> instance
    (j/from-java-shallow {})
    #_(select-keys [:fragment :namespace])))

(defmethod j/from-java org.semanticweb.owlapi.model.OWLClass
  [instance]
  (j/from-java (.getIRI instance)))

(defn axiom-from-java
  [instance ks]
  (-> instance
    (j/from-java-shallow {})
    (select-keys (conj ks :axiomType))
    (->> (map-vals j/from-java))))

(defmethod j/from-java org.semanticweb.owlapi.model.OWLSubClassOfAxiom
  [instance]
  (axiom-from-java instance [:subClass :superClass]))

(defmethod j/from-java org.semanticweb.owlapi.model.OWLDeclarationAxiom
  [instance]
  (axiom-from-java instance [:entity]))

(defmethod j/from-java org.semanticweb.owlapi.model.AxiomType
  [instance]
  (-> instance
    (j/from-java-shallow {})
    :name
    keyword))

(defmethod j/from-java org.semanticweb.owlapi.model.OWLAxiomChange
  [instance]
  (-> instance
    (j/from-java-shallow {})
    (select-keys [:changeData,
                  :changeRecord,
                  :ontology,
                  :removedAxiom,
                  :isAddAxiom,
                  :isAxiomChange,
                  :isImportChange,
                  :isRemoveAxiom,
                  :reverseChange])
    (->> (map-vals j/from-java))))

(defmethod j/from-java org.semanticweb.owlapi.model.OWLOntology
  [instance]
  (-> instance
    (j/from-java-shallow {:omit #{:OWLOntologyManager}})
    (select-keys [:ontologyID])
    (update :ontologyID (fn [id]
                          (-> id
                            (j/from-java-shallow {})
                            :defaultDocumentIRI
                            (.get)
                            j/from-java)))))
