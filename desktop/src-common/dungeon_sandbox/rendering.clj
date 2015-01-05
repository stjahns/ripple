(ns dungeon-sandbox.rendering
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [dungeon-sandbox.components :as c]
            [brute.entity :as e]
            [brute.system :as s])
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion SpriteBatch]
           [com.badlogic.gdx.graphics Texture]
           [com.badlogic.gdx Gdx]
           [dungeon_sandbox.components Position SpriteRenderer TiledMapRendererComponent]))

(def pixels-per-unit 32)

(defn create-tiled-map-component
  [path unit]
  (let [renderer (orthogonal-tiled-map path unit)]
    (c/->TiledMapRendererComponent renderer)))

(defn create-sprite-renderer
  [texture-path tile-coord-x tile-coord-y tile-size-x tile-size-y]
  (let [texture-region (-> (or (u/load-asset texture-path Texture)
                               (Texture. texture-path))
                           (TextureRegion. tile-coord-x tile-coord-y tile-size-x tile-size-y))]
    (c/->SpriteRenderer texture-region)))

(defn- create-camera []
  (let [camera (orthographic)
        width (/ (.getWidth Gdx/graphics) pixels-per-unit)
        height (/ (.getHeight Gdx/graphics) pixels-per-unit)]
    (.setToOrtho camera false width height)
    (.update camera)
    camera))

(defn start
  "Start this system"
  [system]
  (assoc system :renderer {:sprite-batch (SpriteBatch.)
                           :camera (create-camera)}))

(defn- render-maps
  "Render any tiled map renderer components"
  [system]
  (doseq [entity (e/get-all-entities-with-component system TiledMapRendererComponent)]
    (let [tiled-map-component (e/get-component system entity TiledMapRendererComponent)
          tiled-map-renderer (:tiled-map-renderer tiled-map-component)
          camera (:camera (:renderer system))]
      (.setView tiled-map-renderer camera)
      (.render tiled-map-renderer))))

(defn- render-sprites
  "Render sprites for each SpriteRenderer component"
  [system]
  (let [sprite-batch (:sprite-batch (:renderer system))
        camera (:camera (:renderer system))
        ]
    (.setProjectionMatrix sprite-batch (.combined camera))
    (.begin sprite-batch)
    (doseq [entity (e/get-all-entities-with-component system SpriteRenderer)]
      (let [sprite-renderer (e/get-component system entity SpriteRenderer)
            position (e/get-component system entity Position)
            texture (:texture sprite-renderer)
            x (float (/ (:x position) pixels-per-unit))
            y (float (/ (:y position) pixels-per-unit))]
        (.draw sprite-batch texture x y (float 1) (float 1))))
    (.end sprite-batch)))

(defn process-one-game-tick
  "Render stuff"
  [system _]
  (let [camera (:camera (:renderer system))]

    ;; Temp..
    (when (key-pressed? :w)
      (.translate camera 0 1)
      (.update camera))

    (when (key-pressed? :s)
      (.translate camera 0 -1)
      (.update camera))

    (when (key-pressed? :a)
      (.translate camera -1 0)
      (.update camera))

    (when (key-pressed? :d)
      (.translate camera 01 0)
      (.update camera))

    (render-maps system)
    (render-sprites system))
  system)
