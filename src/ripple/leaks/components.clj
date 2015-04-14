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

(defn- blob-switch-collision
  "Set the collision filter mask on the given Box2D body for the blob"
  [body]
  (let [new-filter (physics/create-filter :category 2 :mask 0)]
    (doseq [fixture (.getFixtureList body)]
      (.setFilterData fixture new-filter))))

(defn- blob-on-splat
  "When a blob hits a ship system: 
      play a sound 
      swap the sprite
      set collision filter mask on blob fixtures to collide with nothing except mop"
  [system entity]
  (let [blob (e/get-component system entity 'Blob)
        body (:body (e/get-component system entity 'PhysicsBody))
        new-filter (physics/create-filter :category 2 :mask 0)]
    (.play (:splash-sound blob))
    (doto body
      (.setGravityScale 0)
      (.setAngularVelocity 0)
      (.setLinearVelocity 0 0)
      (blob-switch-collision))
    (e/update-component system entity 'SpriteRenderer #(assoc % :texture (:splat-sprite blob)))))

(defn- blob-on-collide
  "When blob hits a ShipSystem, send it an on-blob-splat event
  and invoke blob-on-splat for the blob"
  [system entity event]
  (let [other-entity (-> (:other-fixture event)
                         (.getUserData)
                         (:entity))]
    (-> system
        (when-> (e/get-component system other-entity 'ShipSystem)
                (blob-on-splat entity)
                (event/send-event other-entity {:event-id :on-blob-splat})))))

(c/defcomponent Blob
  :fields [:splash-sound {:asset true}
           :splat-sprite {:asset true}]
  :on-event [:on-collision-start blob-on-collide])

;;================================================================================
;; MopHead Component
;;================================================================================

(defn- mophead-on-enter
  [system entity {:keys [entering-fixture]}]
  (if-let [entering-entity (:entity (.getUserData entering-fixture))]
    (let [component (e/get-component system entity 'MopHead)]
      (.play (:pop-sound component) 1 1 0)
      (c/destroy-entity system entering-entity))
    system))

(c/defcomponent MopHead
  :on-event [:on-trigger-entered mophead-on-enter]
  :fields [:pop-sound {:asset true}])

;;================================================================================
;; Explosion Component
;;================================================================================

(defn- explosion-on-finished
  "Destroy the explosion entity when the animation finishes"
  [system entity event]
  (c/destroy-entity system entity))

(c/defcomponent Explosion
  :on-event [:animation-finished explosion-on-finished])

;;================================================================================
;; ShipSystem Component
;;================================================================================

(defn- destroy-ship-system
  "Play explosion sound, instantiate explosion prefab, destroy entity"
  [system entity]
  (let [ship-system (e/get-component system entity 'ShipSystem)
        transform (e/get-component system entity 'Transform)
        position (t/get-position system transform)
        [px py] [(.x position) (.y position)]] 
    (-> (a/get-asset system (:explosion-sound ship-system))
        (.play))
    (-> system
        (c/destroy-entity entity)
        (prefab/instantiate (:explosion-prefab ship-system) 
                            {:transform {:position [px py]}})
        (event/send-event-to-tag "GameController" 
                                 {:event-id :on-system-destroyed
                                  :system-name (:system-name ship-system)}))))

(defn- system-on-blobbed
  "Event handler for when a Blob splats on a ship system"
  [system entity event]
  (let [ship-system (e/get-component system entity 'ShipSystem)
        hit-count (inc (:hit-count ship-system))]
    (-> system 
        (e/update-component entity 'ShipSystem #(assoc % :hit-count hit-count))
        (when-> (> hit-count (:max-hit-count ship-system))
                (destroy-ship-system entity)))))

(c/defcomponent ShipSystem
  :fields [:explosion-sound {:default "ExplosionSound"}
           :explosion-prefab {:default "Explosion"}
           :hit-count {:default 0}
           :max-hit-count {:default 1}
           :system-name {:default "UNNAMED SHIP SYSTEM"}]
  :on-event [:on-blob-splat system-on-blobbed])

;;================================================================================
;; GameController Component
;;================================================================================

(defn- on-game-over
  "Tell player to die, show end game text"
  [system entity]
  (-> system
      (event/send-event-to-tag "Player" {:event-id :player-death})
      (event/send-event-to-tag "GameText" {:event-id :show-text
                                           :text "LIFE SUPPORT FAILURE! PRESS 'R' TO RESTART"})))

(defn- on-system-destroyed
  "Display SYSTEM DESTROYED text, and count down :system-count'
  Game over when :system-count is 0"
  [system entity event]
  (let [remaining-systems (-> (e/get-component system entity 'GameController)
                              :system-count
                              dec)]
    (-> system 
        (e/update-component entity 'GameController #(assoc % :system-count remaining-systems))
        (event/send-event-to-tag "GameText" {:event-id :show-text
                                             :text (str (:system-name event) " DESTROYED!")})
        (when-> (< remaining-systems 1)
                (on-game-over entity)))))

(defn- restart-game
  "Set :restart flag to inform core that we should do a full restart"
  [system entity event]
  (assoc system :restart true))

(c/defcomponent GameController
  :fields [:system-count {:default 2}]
  :on-event [:on-system-destroyed on-system-destroyed
             :restart-game restart-game])

;;================================================================================
;; LeakEmitter Component
;;================================================================================

(defn- emit-prefab
  "Spawns the prefab at the emitter's location, with a random velocity"
  [system entity]
  (let [emitter (e/get-component system entity 'LeakEmitter)
        [direction-x direction-y] (:emit-direction emitter)
        [x y] (:position (e/get-component system entity 'Transform))
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
    (-> system
        (if-> (> elapsed-time (:emit-interval emitter)) 
              ;; Reset timer and spawn prefab
              (-> (e/update-component entity 'LeakEmitter #(assoc % :emit-timer 0))
                  (emit-prefab entity))
              ;; Advance timer
              (e/update-component entity 'LeakEmitter #(assoc % :emit-timer elapsed-time))))))

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
  :component-defs ['LeakEmitter 'MopHead 'Blob 'ShipSystem 'Explosion 'GameController])
