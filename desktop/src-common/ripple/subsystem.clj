(ns ripple.subsystem
  (:use [pallet.thread-expr])
  (:require [ripple.components :as c]
            [ripple.assets]
            [brute.entity :as e]))

(defn register-subsystem [system subsystem]
  (if-let [subsystems (:subsystems system)]
    (assoc system :subsystems (conj subsystems subsystem))
    (assoc system :subsystems [subsystem])))

(defn on-system-event [system event-name]
  (reduce (fn [system subsystem]
            (if-let [event-handler (get subsystem event-name)]
              (event-handler system)
              system))
          system (:subsystems system)))

(defn resolve-component-defs
  "For the list of component symbols and the given namespace, return
  a vector of [component-symbol component-def] pairs"
  [components ns]
  (map #(vector % (var-get (ns-resolve ns %)))
       components))

(defn subsystem-on-pre-render-fn
  "For the given subystem on-pre-render callback and a list of component definitions
  in the subsystem, return a new callback that invokes both the the subsystem callback
  and any component-specific on-pre-render callbacks"
  [on-pre-render component-defs]
  (let [components-with-callbacks (->> component-defs
                                       (filter (fn [[component component-def]] 
                                                 (:on-pre-render component-def))))]
    (fn [system]
      (-> system
          (for-> [[component component-def] components-with-callbacks]
                 (c/foreach-component component (:on-pre-render component-def)))
          ((or on-pre-render identity))))))

(defmacro defsubsystem
  "Interns a symbol 'n in the current namespace bound to the
  enclosed subsystem definition."
  [n & options]
  `(let [options# ~(apply hash-map options)
         subsystem# (dissoc options# :component-defs :asset-defs)
         ns# *ns*

         component-defs# (resolve-component-defs (:component-defs options#) ns#)

         on-pre-render# (subsystem-on-pre-render-fn (:on-pre-render options#)
                                                    component-defs#)

         on-show# (fn [system#]
                    (-> system#
                        (for-> [asset-def# (:asset-defs options#)]
                               (ripple.assets/register-asset-def asset-def# 
                                                                 (var-get (ns-resolve ns# (symbol (str  (name asset-def#) "-asset-def"))))))
                        ;; TODO pick either keywords or symbols and be consistent!
                        (for-> [component-def# (:component-defs options#)]
                               (ripple.components/register-component-def component-def# 
                                                                         (var-get (ns-resolve ns# component-def#))))
                        ((or (:on-show options#) identity))))]

     (intern *ns* '~n (assoc subsystem# 
                             :on-show on-show#
                             :on-pre-render on-pre-render#))))
