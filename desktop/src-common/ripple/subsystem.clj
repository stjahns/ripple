(ns ripple.subsystem)

;; TODO - rather have subsystems registered into system, instead of stored globally like this..
(def subsystems (atom {}))

(defn get-subsystem [system-id]
  (get @subsystems system-id))

(defn remove-subsystem [system-id]
  (swap! subsystems dissoc system-id))

(defn register-subsystem [system-id subsystem]
  (swap! subsystems assoc system-id subsystem))

(defn on-show [system]
  (reduce (fn [system {subsystem-fn :on-show}]
            (if subsystem-fn
              (subsystem-fn system)
              system))
          system (vals @subsystems))) ;; prefer this not to be global... :/

(defn on-render [system]
  (reduce (fn [system {subsystem-fn :on-render}]
            (if subsystem-fn
              (subsystem-fn system)
              system))
          system (vals @subsystems))) ;; prefer this not to be global... :/

(defn on-resize [system]
  (reduce (fn [system {subsystem-fn :on-resize}]
            (if subsystem-fn
              (subsystem-fn system)
              system))
          system (vals @subsystems))) ;; prefer this not to be global... :/

(defmacro defsubsystem
  [n & options]
  `(let [options# ~(apply hash-map options)]
     (register-subsystem (keyword '~n) options#)))
