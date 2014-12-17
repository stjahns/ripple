(ns ripple.move-target
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]))

(defn make [x y]
  (-> (assoc (texture "sprites/target.png")
             :type "move-target"
             :x x
             :y y
             :width 1
             :height 1)))

(defn move-target? [entity]
  (= (:type entity) "move-target"))

(defn clear-target [entities]
  (filter #(not (move-target? %)) entities))

(defn get-move-target
  [entities]
  (first (filter move-target? entities)))
