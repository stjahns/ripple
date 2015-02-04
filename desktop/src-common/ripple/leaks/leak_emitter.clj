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

(defn- blob-on-collide
  [system entity other-fixture]
  (let [blob (e/get-component system entity 'Blob)]
    ;(.play (:splash-sound blob))
    system))

(c/defcomponent Blob
  :fields [:splash-sound {:asset true}]
  :on-event [:on-collision-start blob-on-collide])

;; This could probably be generalized as 'spawner' ?

(c/defcomponent LeakEmitter
  :fields [:emit-interval {:default 1.0}
           :emit-speed {:default 0}
           :emit-direction {:default [0 0]}
           :emit-timer {:default 0}
           :emitted-prefab nil])

(defn- emit-prefab
  "Spawns the prefab at the emitter's location, with a random velocity"
  [system entity]
  (let [emitter (e/get-component system entity 'LeakEmitter)
        [x y] (:position (e/get-component system entity 'Transform))
        [direction-x direction-y] (:emit-direction emitter)
        velocity (-> (Vector2. direction-x direction-y)
                     (.nor)
                     (.scl (float (:emit-speed emitter))))]
    (prefab/instantiate system (:emitted-prefab emitter) {:physicsbody {:x x :y y
                                                                        :velocity-x (.x velocity)
                                                                        :velocity-y (.y velocity)}})))

(defn- update-leak-emitter
  "Increments :emit-timer, resets timer and spawns :emitted-prefab if timer
  exceeds :emit-interval"
  [system entity]
  (let [emitter (e/get-component system entity 'LeakEmitter)
        elapsed-time (+ (.getDeltaTime Gdx/graphics)
                        (:emit-timer emitter))]
    (if (> elapsed-time (:emit-interval emitter))
      (-> system
          (e/update-component entity 'LeakEmitter #(assoc % :emit-timer 0))
          (emit-prefab entity))
      (e/update-component system entity 'LeakEmitter #(assoc % :emit-timer elapsed-time)))))

(defn- update-leak-emitters
  [system]
  (let [entities (e/get-all-entities-with-component system 'LeakEmitter)]
    (reduce update-leak-emitter system entities)))

(s/defsubsystem emitters
  :on-show
  (fn [system]
    (c/register-component-def 'LeakEmitter LeakEmitter)
    (c/register-component-def 'Blob Blob)
    system)
  :on-pre-render update-leak-emitters)
