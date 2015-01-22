(ns ripple.components
  (:require
   [brute.entity :refer :all])
  (:import
   [clojure.lang PersistentArrayMap]))

;;
;; Tell brute.entity to use value for :type in a component map
;; to differentiate component types
;;
(defmethod get-component-type PersistentArrayMap
  [component]
  (:type component))

(def component-defs (atom {}))

(defn get-component-def [component-symbol]
  (get @component-defs component-symbol))

(defn remove-component-def [component-symbol]
  (swap! component-defs dissoc component-symbol))

(defn register-component-def [component-symbol create-fn]
  (swap! component-defs assoc component-symbol create-fn))

(defmacro defcomponent
  [n & options]
  `(let [options# ~(apply hash-map options)
         create-fn# (:create options#)]
     (register-component-def '~n (assoc options#
                                        :create-component #(assoc (create-fn# %1 %2)
                                                                  :type '~n)))))

(defn create-component [system component-symbol params]
  (-> (get-component-def component-symbol)
      (get :create-component)
      (apply [system params])))

(defcomponent Transform
  :create
  (fn [system {:keys [:position :rotation :scale]
               :or {position [0 0]
                    rotation 0
                    scale [1 1]}}]
    {:position position
     :rotation rotation
     :scale scale})) 
