(ns ripple.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.player :as player]
            [ripple.rendering :as rendering]
            [ripple.sprites :as sprites]
            [ripple.physics :as physics]
            [ripple.audio :as audio]
            [ripple.components :as c]
            [ripple.assets :as a]
            [ripple.subsystem :as subsystem]
            [ripple.prefab :as prefab]
            [ripple.tiled-map :as tiled-map]
            [brute.entity :as e]
            [brute.system :as s]))

(def sys (atom 0))

(defn- start
  "Create all the initial entities with their components"
  [system]
  (let [tile-map (e/create-entity)]
    (-> system
        (prefab/instantiate "PlatformLevel" {}))))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (-> (e/create-system)
        (subsystem/on-system-event :on-show)
        (start)
        (as-> s (reset! sys s)))
    (update! screen :renderer (stage) :camera (orthographic))
    nil)

  :on-touch-down
  (fn [screen entities]
    (reset! sys (-> @sys (subsystem/on-system-event :on-touch-down)))
    nil)

  :on-render
  (fn [screen entities]
    (reset! sys (-> @sys
                    (subsystem/on-system-event :on-pre-render)
                    (subsystem/on-system-event :on-render)))
    nil)

  :on-resize
  (fn [screen entities]
    (subsystem/on-system-event :on-resize @sys)
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
  (subsystem/on-system-event @sys :on-shutdown)
  (on-gl (set-screen! ripple main-screen)))
