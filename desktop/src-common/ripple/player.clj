(ns ripple.player
  (:require
   [play-clj.core :refer :all]
   [brute.entity :as e]
   [ripple.components :refer :all]
   [ripple.subsystem :as s]))

;; An example Player component that handles movement with arrow keys

(defn get-movement-position
  [position direction]
  (case direction
    :up [(:x position) (inc (:y position))]
    :down [(:x position) (dec (:y position))]
    :left [(dec (:x position)) (:y position)]
    :right [(inc (:x position)) (:y position)]
    nil))

(defn- move-player
  [system entity direction]
  (let [player-position (e/get-component system entity 'Position)
        [new-x new-y] (get-movement-position player-position direction)]
    (e/update-component system entity 'Position #(-> % (assoc :x new-x
                                                              :y new-y)))))

(defn- move-player-components
  [system direction]
  (let [player-entities (e/get-all-entities-with-component system 'Player)]
    (reduce #(move-player %1 %2 direction)
            system player-entities)))

(defcomponent Player
  :create
  (fn [system params]))

(defn- get-player-input [system]
  (cond

    (key-pressed? :dpad-up)
    (move-player-components system :up)

    (key-pressed? :dpad-down)
    (move-player-components system :down)

    (key-pressed? :dpad-left)
    (move-player-components system :left)

    (key-pressed? :dpad-right)
    (move-player-components system :right)

    :else system))

(s/defsubsystem player-input
  :on-render get-player-input)
