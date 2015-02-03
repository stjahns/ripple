(ns ripple.rendering
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.subsystem :as s]
            [ripple.components :as c]
            [brute.entity :as e])
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion SpriteBatch]
           [com.badlogic.gdx.graphics Texture]
           [com.badlogic.gdx Gdx]))

(def pixels-per-unit 32)
(def camera-scale 2)

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
    (update-camera-projection camera)))

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
        (assoc-in [:renderer :pixels-per-unit] pixels-per-unit)))

  :on-resize on-viewport-resize

  :on-pre-render
  (fn [system]
    (clear! 0.1 0.1 0.1 1)
    system)

  :on-render render)
