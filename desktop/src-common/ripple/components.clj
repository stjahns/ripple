(ns ripple.components
  (:require
   [brute.entity :refer :all]
   [ripple.assets :as a]
   [ripple.subsystem :as s])
  (:import
   [clojure.lang PersistentArrayMap PersistentHashMap]))

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

(defonce component-defs (atom {}))

(defn get-component-def
  [component-symbol]
  (get @component-defs component-symbol))

(defn register-component-def
  [component-symbol create-fn]
  (swap! component-defs assoc component-symbol create-fn))

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
                                                                                                  :on-event on-event#}
                                                                                                 fields# )
                                                      (init-fn# entity# system# params#)))))))

(defn create-component [system entity component-symbol params]
  (if-let [component-def (get-component-def component-symbol)]
    (-> component-def
        (get :create-component)
        (apply [entity system params]))
    (throw (Exception. (str "Component not defined: " component-symbol)))))

(defcomponent Transform
  :fields [:position {:default [0 0]}
           :rotation {:default 0}
           :scale {:default [1 1]}])

(defn init-component-manager
  "Clear any old component definitions, "
  [system]
  (reset! component-defs {})
  system)

(s/defsubsystem components
  ;;:asset-defs [:texture :texture-region :animation] ;; TODO handle with macro
  :on-show
  (fn [system]
    (register-component-def 'Transform Transform)
    system))
