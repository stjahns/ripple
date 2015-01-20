(ns ripple.physics
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.subsystem :as s]
            [ripple.components :as c]
            [ripple.assets :as asset-db]
            [brute.entity :as e])
  (:import [com.badlogic.gdx.physics.box2d World BodyDef BodyDef$BodyType]
           [com.badlogic.gdx.math Vector2]
           [com.badlogic.gdx Gdx]))

(c/defcomponent PhysicsBody
  :create
  (fn [system {:keys [x y]}]
    (let [world (get-in [:physics :world] system)
          body-def (doto (BodyDef.)
                     (-> .type (set! BodyDef$BodyType/DynamicBody))
                     (-> .position (set x y)))]
      {:body (.createBody world body-def)})))

(defn- create-world []
  (let [gravity (Vector2. 0 -98)
        do-sleep true]
    (World. gravity do-sleep)))

(s/defsubsystem physics
  :on-show
  (fn [system]
    (-> system
        (assoc-in [:physics :world] (create-world)))))

