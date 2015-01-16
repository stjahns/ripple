(ns ripple.rendering
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.subsystem :as s]
            [ripple.components :as c]
            [ripple.asset-database :as asset-db]
            [brute.entity :as e]
            [ripple.tiled-map :as tiled-map])
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion SpriteBatch]
           [com.badlogic.gdx.graphics Texture]
           [com.badlogic.gdx Gdx]))

(def pixels-per-unit 32)

(defn- update-camera-projection [camera]
  (let [width (/ (.getWidth Gdx/graphics) pixels-per-unit)
        height (/ (.getHeight Gdx/graphics) pixels-per-unit)]
    (.setToOrtho camera false width height)))

(defn- create-camera []
  (let [camera (orthographic)]
    (update-camera-projection camera)
    (.update camera)
    camera))

(defn on-viewport-resize
  "Handler for when viewport is resized. Should update camera projection"
  [system]
  (let [camera (:camera (:renderer system))]
    (update-camera-projection camera)))

(s/defsubsystem rendering

  :on-show
  (fn [system]
    (-> system
        (assoc-in [:renderer :camera] (create-camera))
        (assoc-in [:renderer :pixels-per-unit] pixels-per-unit)))

  :on-resize on-viewport-resize
  :on-render identity)
