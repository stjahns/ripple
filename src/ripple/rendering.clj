(ns ripple.rendering
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.subsystem :as s]
            [ripple.components :as c]
            [brute.entity :as e])
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion SpriteBatch]
           [com.badlogic.gdx.graphics Texture]
           [com.badlogic.gdx.math Matrix4]
           [com.badlogic.gdx Gdx]))

(def pixels-per-unit 32)
(def camera-scale 2)

(defn screen-to-world
  "Convert a pair of screen coordinates to a pair of world coordinates
   with the current camera transform and pixels-per-unit ratio"
  [system screen-x screen-y]
  (let [pixels-per-unit (get-in system [:renderer :pixels-per-unit])

        screen-width (.getWidth Gdx/graphics)
        screen-height (.getHeight Gdx/graphics)

        screen-x (- screen-x (/ screen-width 2))
        screen-y (- (/ screen-height 2) screen-y) ;; pixels relative to screen center

        camera (get-in system [:renderer :camera])
        camera-x (-> camera .position .x)
        camera-y (-> camera .position .y) ;; world space of screen center

        world-x (+ (/ screen-x pixels-per-unit) camera-x)
        world-y (+ (/ screen-y pixels-per-unit) camera-y)]
    [(float world-x) (float world-y)]))

(defn update-camera-position [camera x y]
  (let [position (.position camera)]
    (doto position
      (-> .x (set! x))
      (-> .y (set! y)))))

(defn- update-camera-projection [camera]
  (let [width (/ (.getWidth Gdx/graphics) (* camera-scale pixels-per-unit))
        height (/ (.getHeight Gdx/graphics) (* camera-scale pixels-per-unit))]
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
    (update-camera-projection camera)
    (assoc-in system [:renderer :screen-matrix] 
              (doto (Matrix4.) 
                (.setToOrtho2D 0 0 (.getWidth Gdx/graphics) (.getHeight Gdx/graphics)))) ))

(defn render
  [system]
  "Invokes all registered render callbacks in increasing order"
  (let [camera (get-in system [:renderer :camera])
        render-callbacks (get-in system [:renderer :render-callbacks])]
    (.update camera)
    (reduce (fn [system [_ callback]] (callback system))
            system render-callbacks)))

(defn register-render-callback [system render-fn order]
  "Register a render callback, where callbacks will be called in increasing order"
  (let [render-callbacks (get-in system [:renderer :render-callbacks])
        new-callback [order render-fn]]
    (assoc-in system [:renderer :render-callbacks]
              (sort #(< (first %1) (first %2))
                    (conj render-callbacks new-callback)))))

(s/defsubsystem rendering
  :on-show
  (fn [system]
    (-> system
        (assoc-in [:renderer :camera] (create-camera))
        (assoc-in [:renderer :screen-matrix] 
                  (doto (Matrix4.) 
                    (.setToOrtho2D 0 0 (.getWidth Gdx/graphics) (.getHeight Gdx/graphics))))
        (assoc-in [:renderer :pixels-per-unit] pixels-per-unit)))

  :on-resize on-viewport-resize

  :on-pre-render
  (fn [system]
    (clear! 0 0 0 1)
    system)

  :on-render render)
