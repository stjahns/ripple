(ns ripple.subsystem)

;; TODO - rather have subsystems registered into system, instead of stored globally like this..

(def subsystems (atom {}))

(defn get-subsystem [system-id]
  (get @subsystems system-id))

(defn remove-subsystem [system-id]
  (swap! subsystems dissoc system-id))

(defn register-subsystem [system-id subsystem]
  (swap! subsystems assoc system-id subsystem))

(defn on-system-event [system event-name]
  (reduce (fn [system subsystem]
            (if-let [event-handler (get subsystem event-name)]
              (event-handler system)
              system))
          system (vals @subsystems)))

(defmacro defsubsystem
  [n & options]
  `(let [options# ~(apply hash-map options)]
     (register-subsystem (keyword '~n) options#)))
