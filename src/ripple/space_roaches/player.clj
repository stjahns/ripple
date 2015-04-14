(ns ripple.space-roaches.player
  (:require [play-clj.core :refer :all]
            [brute.entity :as e]
            [ripple.components :as c]
            [ripple.rendering :as r]
            [ripple.sprites :as sprites]
            [ripple.prefab :as prefab]
            [ripple.assets :as a]
            [ripple.subsystem :as s])
  (:import [com.badlogic.gdx.math Vector2]
           [com.badlogic.gdx Input$Keys]
           [com.badlogic.gdx Gdx]))

(def cardinal-directions-to-aim-states
  (map (fn [[dir anim]] [(.nor dir) anim])
       [[(Vector2. 0 1) :aim-up]
        [(Vector2. 1 1) :aim-up-forward]
        [(Vector2. 1 0) :aim-forward]
        [(Vector2. 1 -1) :aim-down-forward]
        [(Vector2. 0 -1) :aim-down]
        [(Vector2. -1 -1) :aim-down-forward]
        [(Vector2. -1 0) :aim-forward]
        [(Vector2. -1 1) :aim-up-forward]]))

(defn- get-aim-state-for-direction
  "Sorts cardinal directions by distance from direction, and
  returns the aim state for the closest direction"
  [direction]
  (let [sorted-directions-to-anims (->> cardinal-directions-to-aim-states
                                        (map (fn [[dir anim]]
                                               [(-> (.sub (.cpy dir) direction)
                                                    (.len2))
                                                anim]))
                                        (sort-by first))]
    (-> sorted-directions-to-anims
        (first)
        (second))))

(defn- get-player-aim-state
  [system entity]
  (let [[mouse-x mouse-y] (r/screen-to-world system (.getX Gdx/input) (.getY Gdx/input))
        [player-x player-y] (:position (e/get-component system entity 'Transform))
        player-to-mouse (Vector2. (- mouse-x player-x)
                                  (- mouse-y player-y))]
    (get-aim-state-for-direction player-to-mouse)))

(defn- get-player-aim-direction
  [system entity]
  (let [[mouse-x mouse-y] (r/screen-to-world system (.getX Gdx/input) (.getY Gdx/input))
        [player-x player-y] (:position (e/get-component system entity 'Transform))
        player-to-mouse (Vector2. (- mouse-x player-x)
                                  (- mouse-y player-y))]
    (.nor player-to-mouse)))

(defn- get-player-walk-state
  [system entity]
  (let [player (e/get-component system entity 'Player)
        v2 (-> (e/get-component system entity 'PhysicsBody)
               (:body)
               (.getLinearVelocity)
               (.len2))]
    (if (> v2 0.1) :walking :standing)))

(def state-to-anim
  {[:standing :aim-forward] :idle-animation
   [:standing :aim-up-forward] :idle-up-forward-animation
   [:standing :aim-up] :idle-up-animation
   [:standing :aim-down-forward] :idle-down-forward-animation
   [:standing :aim-down] :idle-down-animation
   [:walking :aim-forward] :walk-animation
   [:walking :aim-up-forward] :walking-up-forward-animation
   [:walking :aim-up] :walking-up-animation
   [:walking :aim-down-forward] :walking-down-forward-animation
   [:walking :aim-down] :walking-down-animation})

(defn enter-state
  [system entity state]
  (let [anim-ref (get state-to-anim state)
        anim (get (e/get-component system entity 'Player) anim-ref)]
    (sprites/play-animation system entity anim)))

(defn- get-player-state [system entity]
  (let [walk-state (get-player-walk-state system entity)
        aim-state (get-player-aim-state system entity)]
    [walk-state aim-state]))

(defn- update-state [system entity]
  (let [player (e/get-component system entity 'Player)
        old-state (:state player)
        new-state (get-player-state system entity)]
    (if (not (= old-state new-state))
      (-> system
          (enter-state entity new-state)
          (e/update-component entity 'Player #(-> % (assoc :state new-state))))
      system)))

(defn- update-player-aim
  "Flip x scale appropriately based on mouse direction from player"
  [system entity]
  (let [[mouse-x mouse-y] (r/screen-to-world system (.getX Gdx/input) (.getY Gdx/input))
        [player-x player-y] (:position (e/get-component system entity 'Transform))
        facing-left (< (- mouse-x player-x) 0)]
    (-> system
        (e/update-component entity 'SpriteRenderer #(assoc % :flip-x facing-left)))))

(defn- get-move-direction []
  "Return normalized movement direction for whatever movement keys are currently depressed"
  (let [keys-to-direction {Input$Keys/DPAD_UP (Vector2. 0 1)
                           Input$Keys/DPAD_DOWN (Vector2. 0 -1)
                           Input$Keys/DPAD_LEFT (Vector2. -1 0)
                           Input$Keys/DPAD_RIGHT (Vector2. 1 0)
                           Input$Keys/W (Vector2. 0 1)
                           Input$Keys/S (Vector2. 0 -1)
                           Input$Keys/A (Vector2. -1 0)
                           Input$Keys/D (Vector2. 1 0)}]
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

(defn- update-player-movement
  [system entity]
  (let [direction (get-move-direction)]
    (when (> (.len2 direction) 0)
      (apply-movement-force system entity direction))
    system))

(defn- player-fire
  [system entity]
  (let [player (e/get-component system entity 'Player)
        bullet-prefab (:bullet-prefab player)
        fire-sound (:fire-sound player)
        aim-direction (get-player-aim-direction system entity)
        bullet-offset (float (:bullet-offset player))
        bullet-speed (float (:bullet-speed player))
        [x y] (:position (e/get-component system entity 'Transform))
        bullet-origin (.add (Vector2. x y)
                            (.scl (.cpy aim-direction) bullet-offset))
        bullet-velocity (.scl aim-direction bullet-speed)]
    (.play fire-sound)
    (prefab/instantiate system bullet-prefab {:physicsbody {:x (.x bullet-origin)
                                                            :y (.y bullet-origin)
                                                            :velocity-x (.x bullet-velocity)
                                                            :velocity-y (.y bullet-velocity)}})))
(defn- handle-mouse-input
  [system]
  (let [entity (-> (e/get-all-entities-with-component system 'Player)
                   (first))]
    (player-fire system entity)))

(defn- update-camera-target
  [system entity]
  (let [[x y] (:position (e/get-component system entity 'Transform))
        camera (get-in system [:renderer :camera])]
    (r/update-camera-position camera x y))
  system)

(defn- update-player
  [system entity]
  (let [player (e/get-component system entity 'Player)]
    (-> system
        (update-state entity)
        (update-player-aim entity)
        (update-player-movement entity)
        (update-camera-target entity))))

(c/defcomponent Player
  :on-pre-render update-player
  :fields [:move-force {:default 100}
           :bullet-prefab nil
           :bullet-speed {:default 100}
           :bullet-offset {:default 1}
           :fire-sound {:asset true}
           :walk-animation {:asset true}
           :idle-animation {:asset true}
           :idle-down-forward-animation {:asset true}
           :idle-down-animation {:asset true}
           :idle-up-animation {:asset true}
           :idle-up-forward-animation {:asset true}
           :walking-down-forward-animation {:asset true}
           :walking-down-animation {:asset true}
           :walking-up-animation {:asset true}
           :walking-up-forward-animation {:asset true}])

(s/defsubsystem player
  :component-defs ['Player]
  :on-touch-down handle-mouse-input)
