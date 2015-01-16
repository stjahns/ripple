(ns ripple.input
  (:require [play-clj.core :refer :all]
            [ripple.components :as c]
            [brute.entity :as e]
            [brute.system :as s]
            [ripple.asset-database :as asset-db])
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion SpriteBatch]
           [com.badlogic.gdx.graphics Texture]))

(defn get-movement-position
  [position direction]
  (case direction
    :up [(:x position) (inc (:y position))]
    :down [(:x position) (dec (:y position))]
    :left [(dec (:x position)) (:y position)]
    :right [(inc (:x position)) (:y position)]
    nil))

(defn- move-player
  [system player direction]
  (let [player-position (e/get-component system player 'Position)
        [new-x new-y] (get-movement-position player-position direction)]
    (e/update-component system player 'Position #(-> % (assoc :x new-x
                                                             :y new-y)))))

(defn- move-player-components
  [system direction]
  (let [player-entities (e/get-all-entities-with-component system 'Player)]
    (reduce #(move-player %1 %2 direction)
            system player-entities)))

(defn play-animation [system entity animation]
  (e/update-component system entity 'AnimationController #(-> % (assoc :animation animation
                                                                       :start-time (/ (com.badlogic.gdx.utils.TimeUtils/millis) 1000.)
                                                                       :playing true))))

(defn- play-anim-on-all-controllers [system anim-name]
  (let [anim-controller-entities (e/get-all-entities-with-component system 'AnimationController)
        animation (asset-db/get-asset system anim-name)]
    (reduce #(play-animation %1 %2 animation)
            system anim-controller-entities)))

(defn process-one-game-tick
  "Handle player movement..."
  [system delta]
  (cond

    (key-pressed? :dpad-up)
    (move-player-components system :up)

    (key-pressed? :dpad-down)
    (move-player-components system :down)

    (key-pressed? :dpad-left)
    (move-player-components system :left)

    (key-pressed? :dpad-right)
    (move-player-components system :right)

    (key-pressed? :p)
    (play-anim-on-all-controllers system "PlayerWalk")

    :else system))
