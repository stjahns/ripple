(ns ripple.player
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [ripple.components :refer :all]))

(defcomponent Player
  :create
  (fn [system params]))

(defn make [x y]
  (let [player-sheet (texture "sprites/player.png")
        player-tiles (texture! player-sheet :split 32 32)
        player-image (texture (aget player-tiles 0 0))
        player-image (assoc player-image :x x :y y :width 1 :height 1)]
    player-image))

(defn player? [entity]
  (= (:type entity) "player"))

(defn get-player
  [entities]
  (first (filter player? entities)))

;; ABOVE - DEPRECATED...

