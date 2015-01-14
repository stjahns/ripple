(ns ripple.core
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion Sprite]
           [com.badlogic.gdx.graphics Texture]
           [com.badlogic.gdx.maps MapLayer]
           [com.badlogic.gdx.maps.tiled TmxMapLoader]
           )
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.move-target :as move-target]
            [ripple.player :as player]
            [ripple.rendering :as rendering]
            [ripple.components :as c]
            [ripple.input :as input]
            [ripple.asset-database :as asset-db]
            [ripple.prefab :as prefab]
            [brute.entity :as e]
            [brute.system :as s]))

(def sys (atom 0))

(defn- load-map-file
  "Return a TiledMap instance for the given path"
  [path]
  (let [map-loader (TmxMapLoader.)]
   (.load map-loader path)))

(defn- get-object-layers
  [tiled-map]
  (filter (fn [layer] (= (type layer) MapLayer))
   (.getLayers tiled-map)))

(defn- create-player
  [system x y]
  (let [player (e/create-entity)]
    (-> system
        (e/add-entity player)
        (e/add-component player {:type 'Player})
        (e/add-component player {:type 'Position
                                 :x x
                                 :y y})
        (e/add-component player (rendering/create-sprite-renderer "sprites/PlayerV2.png" 0 0 32 32)))))

(defn- get-map-objects [tiled-map]
  (let [object-layer (first (get-object-layers tiled-map))]
    (.getObjects object-layer)))

(defn- create-entity-from-map-object
  "For the given map object, create and add an entity with the required components
  (if appropriate) to the ES system"
  [system map-object]
  (let [ellipse (.getEllipse map-object)
        x (.x ellipse)
        y (.y ellipse)]
    ;; TODO remove
    (if (= (.getName map-object) "PlayerSpawn")
      system
      system)))

(defn- create-entities-in-map
  [system tiled-map]
  (let [map-objects (get-map-objects tiled-map)]
    (reduce (fn [system map-object]
              (create-entity-from-map-object system map-object))
            system map-objects)))

(defn- start
  "Create all the initial entities with their components"
  [system]
  (let [tile-map (e/create-entity)]
    (-> system
        (create-entities-in-map (:tiled-map system))
        (prefab/instantiate "Player" {:position {:x 200 :y 400}})
        (e/add-component tile-map (rendering/create-tiled-map-component "platform.tmx" (/ 1 32))))))

(defn- create-systems
  "Register all the system functions"
  [system]
  (-> system
      (asset-db/start ["resources/example.yaml"])
      (rendering/start)
      (assoc :tiled-map (load-map-file "platform.tmx"))
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

(defgame ripple
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                         (catch Exception e
                           (.printStackTrace e)
                           (set-screen! main-screen blank-screen)))))

(defn reload []
  (on-gl (set-screen! ripple main-screen)))
