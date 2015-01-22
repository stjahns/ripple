(ns ripple.player
  (:require [play-clj.core :refer :all]
            [brute.entity :as e]
            [ripple.components :refer :all]
            [ripple.sprites :as sprites]
            [ripple.assets :as a]
            [ripple.subsystem :as s])
  (:import [com.badlogic.gdx.math Vector2]
           [com.badlogic.gdx Input$Keys]
           [com.badlogic.gdx Gdx]
           ))

;; An example Player component that handles physics-based movement with arrow keys

(defcomponent Player
  :create
  (fn [system {:keys [move-force walk-animation idle-animation]}]
    {:state :standing
     :walk-animation (a/get-asset system walk-animation)
     :idle-animation (a/get-asset system idle-animation)
     :move-force move-force}))

(defn- get-move-direction []
  "Return normalized movement direction for whatever movement keys are currently depressed"
  (let [keys-to-direction {Input$Keys/DPAD_UP (Vector2. 0 1)
                           Input$Keys/DPAD_DOWN (Vector2. 0 -1)
                           Input$Keys/DPAD_LEFT (Vector2. -1 0)
                           Input$Keys/DPAD_RIGHT (Vector2. 1 0)}]
    (-> (reduce (fn [move-direction [keycode direction]]
                  (if (.isKeyPressed Gdx/input keycode)
                    (.add move-direction direction)
                    move-direction))
                (Vector2. 0 0) keys-to-direction)
        (.nor))))

(defn- apply-movement-force
  [system entity direction]
  (let [body (-> (e/get-component system entity 'PhysicsBody)
                 (:body))
        force (-> (e/get-component system entity 'Player)
                  (:move-force))
        force (.scl direction (float force))]
    (.applyForceToCenter body force true)))

(defmulti enter-state (fn [_ _ state] state))

(defmethod enter-state :walking
  [system entity state]
  (let [anim (:walk-animation (e/get-component system entity 'Player))]
    (sprites/play-animation system entity anim)))

(defmethod enter-state :standing
  [system entity state]
  (let [anim (:idle-animation (e/get-component system entity 'Player))]
    (sprites/play-animation system entity anim)))

(defn- update-state [system entity]
  (let [player (e/get-component system entity 'Player)
        v2 (-> (e/get-component system entity 'PhysicsBody)
               (:body)
               (.getLinearVelocity)
               (.len2))
        old-state (:state player)
        new-state (if (> v2 0.1) :walking :standing)]
    (if (not (= old-state new-state))
      (-> system
          (enter-state entity new-state)
          (e/update-component entity 'Player #(-> % (assoc :state new-state))))
      system)))

(defn- update-player-movement
  [system entity]
  (let [direction (get-move-direction)]
    (when (> (.len2 direction) 0)
      (apply-movement-force system entity direction))
    system))

(defn- update-player
  [system entity]
  (let [player (e/get-component system entity 'Player)]
    (-> system
        (update-state entity)
        (update-player-movement entity))))

(defn- update-player-components
  [system]
  (let [player-entities (e/get-all-entities-with-component system 'Player)]
    (reduce update-player system player-entities)))

(s/defsubsystem player
  :on-render update-player-components)
