(ns dungeon-sandbox.rendering
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [dungeon-sandbox.components :as c]
            [brute.entity :as e]
            [brute.system :as s])
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion SpriteBatch]
           [com.badlogic.gdx.graphics Texture]
           [dungeon_sandbox.components Position SpriteRenderer]
           )

  )

(defn create-sprite-renderer
  [texture-path tile-coord-x tile-coord-y tile-size-x tile-size-y]
  (let [texture-region (-> (or (u/load-asset texture-path Texture)
                               (Texture. texture-path))
                           (TextureRegion. tile-coord-x tile-coord-y tile-size-x tile-size-y))]
    (c/->SpriteRenderer texture-region)))

(defn start
  "Start this system"
  [system]
  (assoc system :renderer {:sprite-batch (SpriteBatch.)}))

(defn- render-sprites
  "Render sprites for each SpriteRenderer component"
  [system]
  (let [sprite-batch (:sprite-batch (:renderer system))]
    (.begin sprite-batch)
    (doseq [entity (e/get-all-entities-with-component system SpriteRenderer)]
      (let [sprite-renderer (e/get-component system entity SpriteRenderer)
            position (e/get-component system entity Position)
            texture (:texture sprite-renderer)
            x (float (:x position))
            y (float (:y position))]
        (.draw sprite-batch texture x y)))
    (.end sprite-batch)))

(defn process-one-game-tick
  "Render stuff"
  [system _]
  (render-sprites system)
  system)
