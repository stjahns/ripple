(ns dungeon-sandbox.core
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion Sprite]
           [com.badlogic.gdx.graphics Texture])
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [dungeon-sandbox.move-target :as move-target]
            [dungeon-sandbox.player :as player]
            [dungeon-sandbox.rendering :as rendering]
            [dungeon-sandbox.components :as c]
            [dungeon-sandbox.input :as input]
            [brute.entity :as e]
            [brute.system :as s]))

(def sys (atom 0))

(defn- start
  "Create all the initial entities with their components"
  [system]
  (let [player (e/create-entity)
        tile-map (e/create-entity)]
    (-> system
        (e/add-entity player)
        (e/add-component player (c/->Player))
        (e/add-component player (c/->Position 0 0))
        (e/add-component player (rendering/create-sprite-renderer "sprites/player.png" 0 0 32 32))

        (e/add-component tile-map (rendering/create-tiled-map-component "dungeon.tmx" (/ 1 32))))))

(defn- create-systems
  "Register all the system functions"
  [system]
  (-> system
      (rendering/start)
      (s/add-system-fn rendering/process-one-game-tick)
      (s/add-system-fn input/process-one-game-tick)))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (println "Started")
    (-> (e/create-system)
        (start)
        (create-systems)
        (as-> s (reset! sys s)))
    (update! screen :renderer (stage) :camera (orthographic)) ;; not actually used at all ...
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
