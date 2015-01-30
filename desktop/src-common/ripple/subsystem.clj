(ns ripple.subsystem)

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
  `(let [options# ~(apply hash-map options)]
     (intern *ns* '~n options#)))
