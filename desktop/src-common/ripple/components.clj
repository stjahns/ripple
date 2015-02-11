(ns ripple.components
  (:require [brute.entity :refer :all]
            [ripple.assets :as a])
  (:import [clojure.lang PersistentArrayMap PersistentHashMap]
           [com.badlogic.gdx.math Matrix3 Vector2]))

(defn destroy-entity
  "Completely destroys an entity and all its components"
  [system entity]
  (let [components (get-all-components-on-entity system entity)
        destroy-callbacks (->> (map #(:on-destroy %) components)
                               (filter #(not (nil? %))))
        fire-callbacks (fn [system entity]
                            (reduce (fn [system callback] 
                                      (callback system entity))
                                    system destroy-callbacks))]
    (-> system 
        (fire-callbacks entity)
        (kill-entity entity))))

;;
;; Tell brute.entity to use value for :type in a component map
;; to differentiate component types
;;

(defmethod get-component-type PersistentArrayMap
  [component]
  (:type component))

(defmethod get-component-type PersistentHashMap
  [component]
  (:type component))

(defn get-component-def
  "Retrieve a component definition by name from the system"
  [system component-symbol]
  (get-in system [:defs :components component-symbol]))

(defn register-component-def
  "Register the given component definition by name in the system"
  [system component-symbol component-def]
  (assoc-in system [:defs :components component-symbol] component-def))

(defn- init-component-fields
  [system params component fields]
  (reduce (fn [component [field-name field-options]]
            (let [is-asset (:asset field-options)
                  field-param (get params field-name)
                  default-value (:default field-options)
                  field-value (if (and is-asset field-param)
                                (a/get-asset system field-param)
                                field-param)
                  field-value (or field-value default-value)]
              (assoc component field-name field-value)))
          component fields))

(defmacro defcomponent
  [n & options]
  `(let [options# ~(apply hash-map options)
         fields# (apply hash-map (:fields options#))
         on-event# (apply hash-map (:on-event options#))
         on-pre-render# (:on-pre-render options#)
         init-fn# (or (:init options#) (fn [c# _# _# _#] c#))]
     (intern *ns*  '~n (assoc options#
                              :on-pre-render on-pre-render#
                              :create-component (fn [entity# system# params#]
                                                  (-> (#'ripple.components/init-component-fields system#
                                                                                                 params#
                                                                                                 {:type '~n
                                                                                                  :on-event on-event#
                                                                                                  :on-destroy (:on-destroy options#)}
                                                                                                 fields# )
                                                      (init-fn# entity# system# params#)))))))

(defn foreach-component
  "Invoke the given function for each entity in the system with the given
  component, and return the new system after all successive invocations"
  [system component f]
  (reduce f system (get-all-entities-with-component system component)))

(defn create-component [system entity component-symbol params]
  (if-let [component-def (get-component-def system component-symbol)]
    (-> component-def
        (get :create-component)
        (apply [entity system params]))
    (throw (Exception. (str "Component not defined: " component-symbol)))))
