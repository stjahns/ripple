(ns ripple.rendering
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.components :as c]
            [ripple.asset-database :as asset-db]
            [brute.entity :as e]
            [brute.system :as s]
            [ripple.tiled-map :as tiled-map])
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion SpriteBatch]
           [com.badlogic.gdx.graphics Texture]
           [com.badlogic.gdx Gdx]
           [ripple.components Position SpriteRenderer TiledMapRendererComponent]))

(c/defcomponent SpriteRenderer
  :create
  (fn [system params]
    {:texture (asset-db/get-asset system (:texture params))}))

(c/defcomponent AnimationController
  :create
  (fn [system params]
    { :animation nil
      :playing false
      :start-time nil }))

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

(defn start
  "Start this system"
  [system]
  (assoc system :renderer {:sprite-batch (SpriteBatch.)
                           :camera (create-camera)}))

(defn on-viewport-resize
  "Handler for when viewport is resized. Should update camera projection"
  [system]
  (let [camera (:camera (:renderer system))]
    (update-camera-projection camera)))

(defn- render-sprites
  "Render sprites for each SpriteRenderer component"
  [system]
  (let [sprite-batch (:sprite-batch (:renderer system))
        camera (:camera (:renderer system))
        ]
    (.setProjectionMatrix sprite-batch (.combined camera))
    (.begin sprite-batch)
    (doseq [entity (e/get-all-entities-with-component system 'SpriteRenderer)]
      (let [sprite-renderer (e/get-component system entity 'SpriteRenderer)
            position (e/get-component system entity 'Position)
            texture (:texture sprite-renderer)
            x (float (/ (:x position) pixels-per-unit))
            y (float (/ (:y position) pixels-per-unit))]
        (.draw sprite-batch texture x y (float 1) (float 1))))
    (.end sprite-batch)
    system))

;;
;; Not a huge fan of this, would be nicer if you could somehow 'plug in' an AnimationController to a SpriteRenderer
;; so the SpriteRenderer can just query the controller for a texture region as necessary
;;
(defn- update-animation-controller
  [system entity]
  (let [animation-controller (e/get-component system entity 'AnimationController)
        sprite-renderer (e/get-component system entity 'SpriteRenderer)]
    (if-let [animation (:animation animation-controller)]
      (let [anim-time (- (/ (com.badlogic.gdx.utils.TimeUtils/millis) 1000.)
                         (:start-time animation-controller))
            anim-frame (.getKeyFrame animation (float anim-time) true)]
        (e/update-component system entity 'SpriteRenderer #(-> % (assoc :texture anim-frame))))
      system)))

(defn- update-animation-controllers
  [system]
  (let [entities (e/get-all-entities-with-component system 'AnimationController)]
    (reduce update-animation-controller
            system entities)))

(defn process-one-game-tick
  "Render stuff"
  [system _]
  (-> system
      (update-animation-controllers)
      (tiled-map/render-maps) ;; TODO - figure out some system to register callbacks or something
      (render-sprites)))
