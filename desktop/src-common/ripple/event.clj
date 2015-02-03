(ns ripple.event
  (:require [ripple.subsystem :as s]
            [ripple.components :as c]
            [brute.entity :as e]
            [ripple.assets :as a]))

(defn send-event
  "Stores the given event in the event queue for the given entity"
  [system entity event]
  (let [event-hub (e/get-component system entity 'EventHub)]
    (e/update-component system entity 'EventHub
                        #(assoc % :event-queue (conj (:event-queue event-hub) event)))))

;; TODO - events need arguments as well as keywords!
;; option 1 (vector) [:<event-name> arg1 arg2 arg3 ...]
;; option 2 (map) {:event :<event-name> :args [arg1 arg2 ...]}

;; eg: {:event :on-trigger-entered
;;      :params {:entering-fixture <some fixture>}}

(defn dispatch-event
  [system entity event]
  (let [components (e/get-all-components-on-entity system entity)
        event-handlers (->> (map #(get-in % [:on-event (:event-id event)]) components)
                            (filter #(not (nil? %))))]
    (if (> (count event-handlers) 0)
      (reduce (fn [system handler] (handler system entity event))
              system event-handlers)
      system)))

(defn dispatch-events
  [system entity events]
  (reduce (fn [system event] (dispatch-event system entity event))
          system events))

(defn- update-event-hub
  [system entity]
  (let [events (:event-queue (e/get-component system entity 'EventHub))]
    (-> system
        (dispatch-events entity events)
        (e/update-component entity 'EventHub #(assoc % :event-queue [])))))

(defn- update-event-hubs
  [system]
  (let [entities (e/get-all-entities-with-component system 'EventHub)]
    (reduce update-event-hub system entities)))

(c/defcomponent EventHub 
  :fields [:tag {:default "untagged"}
           :outputs {:default []}
           :event-queue {:default []}])

(s/defsubsystem events
  :on-pre-render update-event-hubs
  :on-show
  (fn [system]
    (c/register-component-def 'EventHub EventHub)
    system))
