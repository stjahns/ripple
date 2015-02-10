(ns ripple.components
  (:require
   [brute.entity :refer :all]
   [ripple.assets :as a])
  (:import
   [clojure.lang PersistentArrayMap PersistentHashMap]
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

;;
;; Apparently a Clojure map with more than 8 elements is a PersistentHashMap
;;
(defmethod get-component-type PersistentHashMap
  [component]
  (:type component))

(defn get-component-def
  [system component-symbol]
  (get-in system [:defs :components component-symbol]))

(defn register-component-def
  [system component-symbol create-fn]
  (assoc-in system [:defs :components component-symbol] create-fn))

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
         init-fn# (or (:init options#) (fn [c# _# _# _#] c#))]
     (intern *ns*  '~n (assoc options#
                              :create-component (fn [entity# system# params#]
                                                  (-> (#'ripple.components/init-component-fields system#
                                                                                                 params#
                                                                                                 {:type '~n
                                                                                                  :on-event on-event#
                                                                                                  :on-destroy (:on-destroy options#)}
                                                                                                 fields# )
                                                      (init-fn# entity# system# params#)))))))

(defn create-component [system entity component-symbol params]
  (if-let [component-def (get-component-def system component-symbol)]
    (-> component-def
        (get :create-component)
        (apply [entity system params]))
    (throw (Exception. (str "Component not defined: " component-symbol)))))
