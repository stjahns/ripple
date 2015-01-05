(ns dungeon-sandbox.input
  (:require [play-clj.core :refer :all]
            [dungeon-sandbox.components :as c]
            [brute.entity :as e]
            [brute.system :as s])
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion SpriteBatch]
           [com.badlogic.gdx.graphics Texture]
           [dungeon_sandbox.components Player Position]))

(defn get-movement-position
  [position direction]
  (case direction
    :up [(:x position) (inc (:y position))]
    :down [(:x position) (dec (:y position))]
    :left [(dec (:x position)) (:y position)]
    :right [(inc (:x position)) (:y position)]
    nil))

(defn- move-player
  [system direction]
  (let [player (first (e/get-all-entities-with-component system Player))
        player-position (e/get-component system player Position)
        [new-x new-y] (get-movement-position player-position direction)]
    (-> system
        (e/update-component player Position (fn [position]
                                              (-> position
                                                  (assoc :x new-x)
                                                  (assoc :y new-y)))))))

(defn process-one-game-tick
  "Handle player movement..."
  [system delta]
  (cond

    (key-pressed? :dpad-up)
    (move-player system :up)

    (key-pressed? :dpad-down)
    (move-player system :down)

    (key-pressed? :dpad-left)
    (move-player system :left)

    (key-pressed? :dpad-right)
    (move-player system :right)

    :else system))
