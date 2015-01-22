(ns ripple.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.player :as player]
            [ripple.rendering :as rendering]
            [ripple.sprites :as sprites]
            [ripple.physics :as physics]
            [ripple.components :as c]
            [ripple.assets :as a]
            [ripple.subsystem :as subsystem]
            [ripple.prefab :as prefab]
            [ripple.tiled-map :as tiled-map]
            [brute.entity :as e]
            [brute.system :as s]))

(def sys (atom 0))

;; temp - for testing physics
(defn- spawn-blocks [system]
  (reduce #(prefab/instantiate %1 "BlockPrefab" {:transform {:position [(+ %2 2) 2]}
                                                 :physicsbody {:x (+ %2 2) :y 2}})
          system (range 0 10 1)))

(defn- start
  "Create all the initial entities with their components"
  [system]
  (let [tile-map (e/create-entity)]
    (-> system
        (prefab/instantiate "Player" {:transform {:position [4 6]}
                                      :physicsbody {:x 4 :y 6}})
        (spawn-blocks))))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (-> (e/create-system)
        (subsystem/on-show)
        (start)
        (as-> s (reset! sys s)))
    (update! screen :renderer (stage) :camera (orthographic))
    nil)

  :on-render
  (fn [screen entities]
    (reset! sys (-> @sys
                    (subsystem/on-pre-render)
                    (subsystem/on-render)))
    nil)

  :on-resize
  (fn [screen entities]
    (subsystem/on-resize @sys)
    nil))

(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!)))

(defgame ripple
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                         (catch Exception e
                           (.printStackTrace e)
                           (set-screen! ripple blank-screen)))))

(defn reload []
  (on-gl (set-screen! ripple main-screen)))
