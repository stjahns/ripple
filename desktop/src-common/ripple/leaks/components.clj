(ns ripple.leaks.components
  (:use [pallet.thread-expr])
  (:require [brute.entity :as e]
            [ripple.components :as c]
            [ripple.transform :as t]
            [ripple.event :as event]
            [ripple.rendering :as r]
            [ripple.subsystem :as s]
            [ripple.prefab :as prefab]
            [ripple.assets :as a]
            [ripple.physics :as physics])
  (:import [com.badlogic.gdx.math Vector2]
           [com.badlogic.gdx Input$Keys]
           [com.badlogic.gdx Gdx]))

;;================================================================================
;; Blob Component
;;================================================================================

(defn- blob-on-splat
  "When a blob hits a ship system: 
      play a sound 
      swap the sprite
      set collision filter mask on blob fixtures to collide with nothing except mop (?)"
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
      (-> system
          (blob-on-splat entity)
          (event/send-event other-entity {:event-id :on-blob-splat}))
      system)))

(c/defcomponent Blob
  :fields [:splash-sound {:asset true} ;; TODO - warn / exception when not set!? {:required true}
           :splat-sprite {:asset true}]
  :on-event [:on-collision-start blob-on-collide])

;;================================================================================
;; Explosion Component
;;================================================================================

(defn- explosion-on-finished
  [system entity event]
  (c/destroy-entity system entity))

(c/defcomponent Explosion
  :on-event [:animation-finished explosion-on-finished])

;;================================================================================
;; ShipSystem Component
;;================================================================================

(defn- destroy-ship-system
  "Explode the system (when it's been hit by too many blobs"

  ;; TODO play explosion sound

  [system entity]

  (let [component (e/get-component system entity 'ShipSystem)
        transform (e/get-component system entity 'Transform)
        position (t/get-position system transform)
        [px py] [(.x position) (.y position)]] 
    (-> (a/get-asset system "ExplosionSound")
        (.play))
    (-> system
        (c/destroy-entity entity)
        (prefab/instantiate "Explosion" {:transform {:position [px py]}})
        (event/send-event-to-tag "GameController" {:event-id :on-system-destroyed
                                                   :system-name (:system-name component)}))))

(defn- system-on-blobbed
  "Event handler for when a Blob splats on a ship system"
  [system entity event]
  (let [ship-system (e/get-component system entity 'ShipSystem)
        hit-count (+ 1 (:hit-count ship-system))]
    (-> system 
        (e/update-component entity 'ShipSystem #(assoc % :hit-count hit-count))
        (when-> (> hit-count (:max-hit-count ship-system))
                (destroy-ship-system entity)))))

(c/defcomponent ShipSystem
  :fields [:hit-count {:default 0}
           :max-hit-count {:default 1}
           :system-name {:default "UNNAMED SHIP SYSTEM"}]
  :on-event [:on-blob-splat system-on-blobbed])

;;================================================================================
;; GameController Component
;;================================================================================

(defn- on-game-over
  [system entity]
  (-> system
      (event/send-event-to-tag "Player" {:event-id :player-death})
      (event/send-event-to-tag "GameText" {:event-id :show-text
                                           :text "LIFE SUPPORT FAILURE! PRESS 'R' TO RESTART"})))

(defn- on-system-destroyed
  "When system destroyed, tell GameText to show '<system name> destroyed!'
  Also count down :system-count, game over when 0!"
  [system entity event]
  (let [remaining-systems (- (-> (e/get-component system entity 'GameController)
                                 :system-count)
                             1)]
    (-> system 
        (e/update-component entity 'GameController #(assoc % :system-count remaining-systems))
        (event/send-event-to-tag "GameText" {:event-id :show-text
                                             :text (str (:system-name event) " DESTROYED!")})
        (when-> (< remaining-systems 1)
                (on-game-over entity)))))

(defn- restart-game
  [system entity event]
  (assoc system :restart true)
  ;(ripple.core/restart)
  )

(c/defcomponent GameController
  :fields [:system-count {:default 2}]
  :on-event [:on-system-destroyed on-system-destroyed
             :restart-game restart-game])

;;================================================================================
;; LeakEmitter Component
;;================================================================================

;; This could probably be generalized as 'spawner' ?

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

(c/defcomponent LeakEmitter
  :on-pre-render update-leak-emitter
  :fields [:emit-interval {:default 1.0}
           :emit-speed {:default 0}
           :emit-direction {:default [0 0]}
           :emit-timer {:default 0}
           :emitted-prefab nil])

;;================================================================================
;; Leak Systems
;;================================================================================

(s/defsubsystem leak-systems
  :component-defs ['LeakEmitter 'Blob 'ShipSystem 'Explosion 'GameController])
