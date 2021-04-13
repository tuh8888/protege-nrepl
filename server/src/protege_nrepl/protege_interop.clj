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
  (:require [clojure.java.data :as j]
            [tawny.render :as tr])
  (:import [org.semanticweb.owlapi.model
            OWLAxiom
            OWLAxiomChange
            OWLEntity
            AxiomType
            OWLLiteral
            IRI
            OWLClassExpression
            ClassExpressionType]
           [org.semanticweb.owlapi.change AxiomChangeData]))

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

(defmethod j/from-java OWLAxiom
  [instance]
  (-> instance
    (j/from-java-shallow {:omit #{:signature :ontology
                                  :dataPropertiesInSignature
                                  :objectPropertiesInSignature
                                  :annotationPropertiesInSignature
                                  :classesInSignature
                                  :datatypesInSignature
                                  :nestedClassExpressions
                                  :NNF
                                  :axiomWithoutAnnotations
                                  :individualsInSignature}})
    (->> (map-vals j/from-java))))

(defmethod j/from-java OWLClassExpression
  [instance]
  (-> instance
    (j/from-java-shallow {:omit #{:signature :ontology
                                  :dataPropertiesInSignature
                                  :objectPropertiesInSignature
                                  :annotationPropertiesInSignature
                                  :classesInSignature
                                  :datatypesInSignature
                                  :nestedClassExpressions
                                  :objectComplementOf
                                  :NNF
                                  :complementNNF
                                  :axiomWithoutAnnotations
                                  :individualsInSignature}})
    (->> (map-vals j/from-java))))

(prefer-method j/from-java OWLEntity OWLClassExpression)

(defmethod j/from-java OWLAxiomChange
  [instance]
  (let [{:keys [axiom addAxiom]} (-> instance
                                   (j/from-java-shallow {:omit #{:signature :ontology
                                                                 :dataPropertiesInSignature
                                                                 :objectPropertiesInSignature
                                                                 :annotationPropertiesInSignature
                                                                 :classesInSignature
                                                                 :datatypesInSignature
                                                                 :nestedClassExpressions
                                                                 :NNF
                                                                 :changeData
                                                                 :changeRecord
                                                                 :axiomWithoutAnnotations
                                                                 :individualsInSignature}}))]
    (-> axiom
      j/from-java
      (assoc :change (if addAxiom :added :removed)))
    ))


(defmethod j/from-java AxiomType
  [instance]
  (-> instance
    (.getName)
    keyword))

(defmethod j/from-java ClassExpressionType
  [instance]
  (-> instance
    (.getName)
    keyword))

(defn simplify-axiom [a]
  (-> a
    (select-keys [:axiomType :classExpressionType
                  :filler
                  :change
                  :subClass :superClass
                  :superProperty :subProperty
                  :property :value :subject
                  :entity])))

(defmethod j/from-java OWLEntity
  [instance]
  (tr/as-form instance))

(defmethod j/from-java OWLLiteral
  [instance]
  (tr/as-form instance))

(defmethod j/from-java IRI
  [instance]
  (tr/as-form instance))
