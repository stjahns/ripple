(ns dungeon-sandbox.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [dungeon-sandbox.move-target :as move-target]
            [dungeon-sandbox.player :as player]))


(defn get-movement-position
  [entity direction]
  (case direction
    :up [(:x entity) (inc (:y entity))]
    :down [(:x entity) (dec (:y entity))]
    :left [(dec (:x entity)) (:y entity)]
    :right [(inc (:x entity)) (:y entity)]
    nil))

(defn move
  [screen entity direction]
  (let [[new-x new-y] (get-movement-position entity direction)
        blocked false
        blocked (some-> (tiled-map-layer screen "Walls")
                     (tiled-map-cell new-x new-y)
                     .getTile
                     .getProperties
                     (.get "blocking")
                     (Boolean/parseBoolean))
        ]
    (if (not blocked)
      (assoc entity :x new-x :y new-y))))

(defn handle-click
  [screen entities]
  (let [click-coords (input->screen screen (:input-x screen) (:input-y screen))
        tile-coords [(Math/floor (:x click-coords))
                     (Math/floor (:y click-coords))]
        exisiting-target (move-target/get-move-target entities)
        new-target (move-target/make (first tile-coords) (second tile-coords))]
    (conj (move-target/clear-target entities) new-target)
    ))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen
             :renderer (orthogonal-tiled-map "dungeon.tmx" (/ 1 32))
             :camera (orthographic))
      [(player/make 0 0)])

  :on-render
  (fn [screen entities]
    (clear!)
    (let [player (player/get-player entities)]
      (position! screen (:x player) (:y player)))
    (render! screen entities))

  :on-key-down
  (fn [screen entities]
       (cond
         (= (:key screen) (key-code :dpad-up))
         (move screen (first entities) :up)
         (= (:key screen) (key-code :dpad-down))
         (move screen (first entities) :down)
         (= (:key screen) (key-code :dpad-left))
         (move screen (first entities) :left)
         (= (:key screen) (key-code :dpad-right))
         (move screen (first entities) :right)))

  :on-touch-down handle-click

  :on-resize
  (fn [screen entities]
    (height! screen 10)
    nil))


(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!)))

(defgame dungeon-sandbox
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                         (catch Exception e
                           (.printStackTrace e)
                           (set-screen! main-screen blank-screen)))))

(defn reload []
  (on-gl (set-screen! dungeon-sandbox main-screen)))
