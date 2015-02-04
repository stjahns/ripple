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

(defn- blob-on-splat
  "When a blob hits a ship system: 
      play a sound 
      swap the sprite
      set collision filter mask on blob fixtures to collide with nothing except mop
      TODO increment some hit counter on the ship system
      TODO move to a random position on the ship system?"
  [system entity]
  (let [blob (e/get-component system entity 'Blob)
        body (:body (e/get-component system entity 'PhysicsBody))
        new-filter (physics/create-filter :category 2 :mask 0)
        update-filters (fn [body]
                         (doseq [fixture (.getFixtureList body)]
                           (.setFilterData fixture new-filter)))]
    (.play (:splash-sound blob))
    (doto body
      (.setGravityScale 0)
      (.setAngularVelocity 0)
      (.setLinearVelocity 0 0)
      (update-filters))
    (e/update-component system entity 'SpriteRenderer #(assoc % :texture (:splat-sprite blob)))))

(defn- blob-on-collide
  [system entity event]
  (let [blob (e/get-component system entity 'Blob)
        other-entity (-> (:other-fixture event)
                         (.getUserData)
                         (:entity))
        ship-system (e/get-component system other-entity 'ShipSystem)]
    (if ship-system
      (blob-on-splat system entity)
      system)))

(c/defcomponent Blob
  :fields [:splash-sound {:asset true} ;; TODO - warn / exception when not set!? {:required true}
           :splat-sprite {:asset true}]
  :on-event [:on-collision-start blob-on-collide])

(c/defcomponent ShipSystem)

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
    (c/register-component-def 'ShipSystem ShipSystem)
    system)
  :on-pre-render update-leak-emitters)
