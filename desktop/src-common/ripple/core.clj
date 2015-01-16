(ns ripple.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.player :as player]
            [ripple.rendering :as rendering]
            [ripple.sprites :as sprites]
            [ripple.components :as c]
            [ripple.assets :as asset-db]
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
        (prefab/instantiate "PlatformLevel" {})
        (prefab/instantiate "Player" {:position {:x 200 :y 200}}))))

(defn- create-systems
  "Register all the system functions"
  [system]
  (-> system
      (asset-db/start ["resources/assets.yaml"])))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (println "Started")
    (-> (e/create-system)
        (subsystem/on-show)
        (create-systems)
        (start)
        (as-> s (reset! sys s)))
    (update! screen :renderer (stage) :camera (orthographic))
    nil)

  :on-render
  (fn [screen entities]
    (clear!)
    (reset! sys (subsystem/on-render @sys))
    (reset! sys (s/process-one-game-tick @sys (graphics! :get-delta-time)))
    (render! screen)
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
