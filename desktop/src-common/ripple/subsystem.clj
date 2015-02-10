(ns ripple.subsystem
  (:use [pallet.thread-expr])
  (:require [ripple.components]))

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

(defmacro defsubsystem
  "Interns a symbol 'n in the current namespace bound to the
  enclosed subsystem definition."
  [n & options]
  `(let [options# ~(apply hash-map options)
         subsystem# (dissoc options# :component-defs :asset-defs)
         ns# *ns*
         on-show# (fn [system#]
                    (-> system#
                        (for-> [component-def# (:component-defs options#)]
                               (ripple.components/register-component-def component-def# 
                                                                         (var-get (ns-resolve ns# component-def#))))
                        ((or (:on-show options#) identity))))]
     (intern *ns* '~n (assoc subsystem# :on-show on-show#))))
