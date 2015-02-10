(ns ripple.core
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

(def sys (atom 0))

(defn- start
  "Create all the initial entities with their components"
  [system]
  (-> system
      (prefab/instantiate "LeaksLevel" {})))

(defn- init-system
  "Initialize the ES system and all subsystems"
  []
  (-> (e/create-system)

      (a/init-asset-manager) ;; clears any existing asset defs

      (subsystem/register-subsystem transform/transform)
      (subsystem/register-subsystem event/events)
      (subsystem/register-subsystem rendering/rendering)
      (subsystem/register-subsystem physics/physics)
      (subsystem/register-subsystem prefab/prefabs)
      (subsystem/register-subsystem sprites/sprites)
      (subsystem/register-subsystem audio/audio)
      (subsystem/register-subsystem tiled-map/level)

      ;(subsystem/register-subsystem roaches-player/player)
      (subsystem/register-subsystem leaks-player/player)
      (subsystem/register-subsystem leaks-components/leak-systems)

      (a/load-asset-instance-defs)

      ;; Initialise subsystems
      (subsystem/on-system-event :on-show) ;; TODO rename

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
