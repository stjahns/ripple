(ns ripple.physics
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.subsystem :as s]
            [ripple.components :as c]
            [ripple.rendering :as r]
            [ripple.assets :as asset-db]
            [brute.entity :as e])
  (:import [com.badlogic.gdx.physics.box2d
            World
            BodyDef
            BodyDef$BodyType
            PolygonShape
            CircleShape
            FixtureDef
            Box2DDebugRenderer]
           [com.badlogic.gdx.math Vector2]
           [com.badlogic.gdx Gdx]))

(defmulti get-shape-def (fn [{:keys [shape]}] shape))

(defmethod get-shape-def "circle"
  [{:keys [radius]}]
  (doto (CircleShape.)
    (.setRadius radius)))

(defmethod get-shape-def "box"
  [{:keys [width height]}]
  (doto (PolygonShape.)
    (.setAsBox (/ width 2) (/ height 2))))

(defn- get-fixture-def
  [{:keys [shape density friction is-sensor] :as params}]
  (let [shape-def (get-shape-def params)]
    (doto (FixtureDef.)
      (-> .shape (set! shape-def))
      (-> .density (set! (or density 1)))
      (-> .friction (set! (or friction 1)))
      (-> .isSensor (set! (or is-sensor false))))))

(defn- set-fixture-width-height
  "Modifies the 'fixtures' list so the first fixture uses the specified width
  and height (if both non-nil).
  Used by spawners that need to specify custom width + height"
  [fixtures width height]
  (if (and width height)
    (let [first-fixture (assoc (first fixtures)
                               :width width
                               :height height)
          rest (subvec (vec fixtures) 1)]
      (into [first-fixture] rest))
    fixtures))

(c/defcomponent PhysicsBody
  :init
  (fn [component system {:keys [x y fixtures body-type fixed-rotation velocity-x velocity-y
                                width height]}]
    (let [world (get-in system [:physics :world])
          body-type (case body-type
                      "dynamic" BodyDef$BodyType/DynamicBody
                      "kinematic" BodyDef$BodyType/KinematicBody
                      "static" BodyDef$BodyType/StaticBody)
          fixed-rotation (Boolean/valueOf fixed-rotation)
          ;fixtures (set-fixture-width-height fixtures width height)
          body-def (doto (BodyDef.)
                     (-> .type (set! body-type))
                     (-> .position (.set x y))
                     (-> .fixedRotation (set! fixed-rotation)))
          body (doto (.createBody world body-def)
                 (.setLinearVelocity velocity-x velocity-y))]
      (assoc component :body (reduce #(doto %1 (.createFixture (get-fixture-def %2)))
                                     body fixtures)))))

;;
;; TODO - more general way of queuing events :)
;;
(def begin-contact-events (atom []))
(def end-contact-events (atom []))

(defn- create-world []
  (let [gravity (Vector2. 0 -9.8)
        do-sleep true]
    (doto (World. gravity do-sleep)
      (.setContactListener (reify com.badlogic.gdx.physics.box2d.ContactListener
                             (beginContact [this contact]
                               ;; The contact event objects appear to get recycled,
                               ;; so need to copy any data we might care about...
                               (swap! begin-contact-events #(conj % {:fixture-a (.getFixtureA contact)
                                                                     :fixture-b (.getFixtureB contact)})))
                             (endContact [this contact]
                               (swap! end-contact-events #(conj % {:fixture-a (.getFixtureA contact)
                                                                     :fixture-b (.getFixtureB contact)})))
                             (preSolve [this contact oldManifold])
                             (postSolve [this contact impulse]))))))

(defn- update-physics-body
  "Updates the Transform component on the entity with the current position and rotation of the Box2D body"
  [system entity]
  (let [body (-> (e/get-component system entity 'PhysicsBody)
                 (:body))
        body-position (.getPosition body)
        x (.x body-position)
        y (.y body-position)
        rotation (-> (.getAngle body)
                     (Math/toDegrees))]
    (e/update-component system entity 'Transform #(assoc % :position [x y] :rotation rotation))))

(defn- update-physics-bodies
  [system]
  (let [entities (e/get-all-entities-with-component system 'PhysicsBody)]
    (reduce update-physics-body
            system entities)))


(def debug-render? true)

(defn- debug-render*
  [system]
  (when debug-render?
    (let [debug-renderer (get-in system [:physics :debug-renderer])
          world (get-in system [:physics :world])
          camera (get-in system [:renderer :camera])
          projection-matrix (.combined camera)]
      (.render debug-renderer world projection-matrix)))
  system)

(defn- debug-render [system] (debug-render* system) system)

(c/defcomponent AreaTrigger
  :init ;; This needs to happen in add-component so we can get a guid..
  (fn [component system {:keys [x y width height]}]
    (let [world (get-in system [:physics :world])
          body-def (doto (BodyDef.)
                     (-> .type (set! BodyDef$BodyType/StaticBody))
                     (-> .position (.set x y)))
          body (.createBody world body-def)
          fixture (doto (.createFixture body (get-fixture-def {:width width
                                                               :height height
                                                               :is-sensor true
                                                               :shape "box"}))
                    (.setUserData {:entity "SOME UUID"
                                   :on-begin-contact (fn [system other]
                                                       (println "TRIGGER ENTERED"))
                                   :on-end-contact (fn [system other]
                                                     (println "TRIGGER EXITED"))}))]
      (assoc component :body body))))

(defn- handle-contact-events
  [system]

  (let [contact-events @begin-contact-events]
    (reset! begin-contact-events [])
    (doseq [{:keys [fixture-a fixture-b]} contact-events]
      (if-let [data (.getUserData fixture-a)]
        ((:on-begin-contact data) system {}))
      (if-let [data (.getUserData fixture-b)]
        ((:on-begin-contact data) system {}))))

  (let [contact-events @end-contact-events]
    (reset! end-contact-events [])
    (doseq [{:keys [fixture-a fixture-b]} contact-events]
      (if-let [data (.getUserData fixture-a)]
        ((:on-end-contact data) system {}))
      (if-let [data (.getUserData fixture-b)]
        ((:on-end-contact data) system {}))))
  system)

(s/defsubsystem physics

  :on-show
  (fn [system]
    (-> system
        (assoc-in [:physics :world] (create-world))
        (assoc-in [:physics :debug-renderer] (Box2DDebugRenderer.))
        (r/register-render-callback debug-render 2)))

  :on-pre-render
  (fn [system]
    (let [world (get-in system [:physics :world])]
      (.step world (.getDeltaTime Gdx/graphics) 6 2)) ;; TODO - want fixed physics update
    (-> system
        (update-physics-bodies)
        (handle-contact-events))))

;; TODO - how much should we be cleaning up? (.dispose world) etc..
