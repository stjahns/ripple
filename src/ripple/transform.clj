(ns ripple.transform
  (:require [brute.entity :as e]
            [ripple.components :as c]
            [ripple.subsystem :as s])
  (:import [com.badlogic.gdx.math Matrix3 Vector2]))

(c/defcomponent Transform
  :fields [:position {:default [0 0]}
           :rotation {:default 0}
           :scale {:default [1 1]}
           :parent {:default nil}])

(defn get-matrix
  "TODO - instantiating new libGDX matrices on every frame doesn't seem like a good idea"
  [system transform]
  (let [[px py] (:position transform)
        [sx sy] (:scale transform)
        local-matrix (doto (Matrix3.)
                       (.translate px py)
                       (.rotate (:rotation transform))
                       (.scale sx sy))]
    (if (:parent transform)
      (let [parent-transform (e/get-component system (:parent transform) 'Transform)
            parent-matrix (get-matrix system parent-transform)]
        (.mul parent-matrix local-matrix))
      local-matrix)))

(defn get-position
  "Returns a Vector2 for position"
  [system transform]
  (-> (get-matrix system transform)
      (.getTranslation (Vector2.))))

(defn get-scale
  "Returns a Vector2 for scale"
  [system transform]
  (-> (get-matrix system transform)
      (.getScale (Vector2.))))

(defn get-rotation
  "Returns rotation in degrees"
  [system transform]
  (-> (get-matrix system transform)
      (.getRotation)))

(s/defsubsystem transform
  :component-defs ['Transform])
