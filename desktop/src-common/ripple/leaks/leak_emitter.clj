(ns ripple.leaks.leak-emitter
  (:require [brute.entity :as e]
            [ripple.components :as c]
            [ripple.rendering :as r]
            [ripple.subsystem :as s]
            [ripple.prefab :as prefab]
            [ripple.physics :as physics])
  (:import [com.badlogic.gdx.math Vector2]
           [com.badlogic.gdx Input$Keys]
           [com.badlogic.gdx Gdx]))

;; This could be generalized as 'spawner' ?
(c/defcomponent LeakEmitter
  :fields [:emit-interval {:default 1.0}
           :emit-timer {:default 0}
           :emitted-prefab nil])

(defn- update-leak-emitter
  "Increments :emit-timer, resets timer and spawns :emitted-prefab if timer
  exceeds :emit-interval"
  [system entity]
  (let [emitter (e/get-component system entity 'LeakEmitter)
        [x y] (:position (e/get-component system entity 'Transform))
        elapsed-time (+ (.getDeltaTime Gdx/graphics)
                        (:emit-timer emitter))]
    (if (> elapsed-time (:emit-interval emitter))
      (-> system
          (e/update-component entity 'LeakEmitter #(assoc % :emit-timer 0))
          (prefab/instantiate (:emitted-prefab emitter) {:physicsbody {:x x :y y}}))
      (e/update-component system entity 'LeakEmitter #(assoc % :emit-timer elapsed-time)))))

(defn- update-leak-emitters
  [system]
  (let [entities (e/get-all-entities-with-component system 'LeakEmitter)]
    (reduce update-leak-emitter system entities)))

(s/defsubsystem emitters
  :on-show
  (fn [system]
    (c/register-component-def 'LeakEmitter LeakEmitter)
    system)
  :on-pre-render update-leak-emitters)
