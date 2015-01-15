(ns ripple.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.player :as player]
            [ripple.rendering :as rendering]
            [ripple.components :as c]
            [ripple.input :as input]
            [ripple.asset-database :as asset-db]
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
      (asset-db/start ["resources/assets.yaml"])
      (rendering/start)
      (s/add-system-fn rendering/process-one-game-tick)
      (s/add-system-fn input/process-one-game-tick)))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (println "Started")
    (-> (e/create-system)
        (create-systems)
        (start)
        (as-> s (reset! sys s)))
    (update! screen :renderer (stage) :camera (orthographic))
    nil)

  :on-render
  (fn [screen entities]
    (clear!)
    (reset! sys (s/process-one-game-tick @sys (graphics! :get-delta-time)))
    (render! screen)
    nil)

  :on-resize
  (fn [screen entities]
    (rendering/on-viewport-resize @sys)
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
