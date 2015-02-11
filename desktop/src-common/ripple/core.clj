(ns ripple.core
  (:use [pallet.thread-expr])
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.space-roaches.player :as roaches-player]
            [ripple.leaks.player :as leaks-player]
            [ripple.leaks.components :as leaks-components]
            [ripple.rendering :as rendering]
            [ripple.sprites :as sprites]
            [ripple.physics :as physics]
            [ripple.audio :as audio]
            [ripple.components :as c]
            [ripple.assets :as a]
            [ripple.subsystem :as subsystem]
            [ripple.prefab :as prefab]
            [ripple.event :as event]
            [ripple.tiled-map :as tiled-map]
            [ripple.transform :as transform]
            [brute.entity :as e]
            [brute.system :as s]))

(declare shutdown
         restart)

(def subsystems [transform/transform
                 event/events
                 rendering/rendering
                 physics/physics
                 prefab/prefabs
                 sprites/sprites
                 audio/audio
                 tiled-map/level
                 roaches-player/player
                 ;leaks-player/player
                 ])

(def asset-sources [; "leaks/leaks-assets.yaml" ;; 'Leaks' example game
                    "space_roaches/assets.yaml" ;; 'Space Roaches' example game
                    ])

(def sys (atom 0))

(defn- start
  "Create all the initial entities with their components"
  [system]
  (-> system
      ;(prefab/instantiate "LeaksLevel" {})
      (prefab/instantiate "PlatformLevel" {})))

(defn- init-system
  "Initialize the ES system and all subsystems"
  []
  (-> (e/create-system)
      (a/load-asset-instance-defs asset-sources)
      (for-> [subsystem subsystems]
             (subsystem/register-subsystem subsystem))
      (subsystem/on-system-event :on-show)
      (start)))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (reset! sys (init-system))
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
    (when (:restart @sys)
      (shutdown)
      (restart))
    nil)

  :on-resize
  (fn [screen entities]
    (reset! sys (-> @sys (subsystem/on-system-event :on-resize)))
    nil))

(defgame ripple
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

;; Exception Wrapper
(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!)))

(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                         (catch Exception e
                           (.printStackTrace e)
                           (set-screen! ripple blank-screen)))))

(defn shutdown []
  (set-screen! ripple blank-screen)
  (Thread/sleep 100)
  (subsystem/on-system-event @sys :on-shutdown))

(defn restart []
  (set-screen! ripple main-screen))
