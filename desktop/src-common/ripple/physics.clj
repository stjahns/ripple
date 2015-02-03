(ns ripple.physics
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.subsystem :as s]
            [ripple.components :as c]
            [ripple.rendering :as r]
            [ripple.event :as event]
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
  (fn init-physics-body [component entity system {:keys [x y fixtures body-type fixed-rotation velocity-x velocity-y
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
                 (.setLinearVelocity (or 0 velocity-x) 
                                     (or 0 velocity-y)))]
      (assoc component :body (reduce #(doto %1 (.createFixture (get-fixture-def %2)))
                                     body fixtures)))))

;;
;; TODO - more general way of queuing events :)
;;
(def begin-contact-events (atom []))
(def end-contact-events (atom []))

(defn- create-world
  "Initialize Box2D world.
  TODO: check for some kind of Box2DSettings asset for configuration"
  []
  (let [gravity (Vector2. 0 -2.8)
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

(defn get-entities-with-tag [system tag]
  (filter #(= (:tag (e/get-component system % 'EventHub))
              tag)
          (e/get-all-entities-with-component system 'EventHub)))

;; TODO refactor following

(defn- area-trigger-entered
  [system trigger-entity entering-fixture]
  (if-let [event-hub (e/get-component system trigger-entity 'EventHub)]
    (let [outgoing-connections (filter #(= (first %) "on-enter")
                                       (:outputs event-hub))]
      (reduce (fn [system [output-event receiver-tag receiver-event]]
                (event/send-event system
                                  (first (get-entities-with-tag system receiver-tag))
                                  receiver-event))
              system outgoing-connections))
    system))

(defn- area-trigger-exited
  [system trigger-entity entering-fixture]
  (if-let [event-hub (e/get-component system trigger-entity 'EventHub)]
    (let [outgoing-connections (filter #(= (first %) "on-exit")
                                       (:outputs event-hub))]
      (reduce (fn [system [output-event receiver-tag receiver-event]]
                (event/send-event system
                                  (first (get-entities-with-tag system receiver-tag))
                                  receiver-event))
              system outgoing-connections))
    system))

(c/defcomponent AreaTrigger
  :init ;; This needs to happen in add-component so we can get a guid..
  (fn [component entity system {:keys [x y width height]}]
    (let [world (get-in system [:physics :world])
          body-def (doto (BodyDef.)
                     (-> .type (set! BodyDef$BodyType/StaticBody))
                     (-> .position (.set x y)))
          body (.createBody world body-def)
          fixture (doto (.createFixture body (get-fixture-def {:width width
                                                               :height height
                                                               :is-sensor true
                                                               :shape "box"}))
                    (.setUserData {:entity entity
                                   :on-begin-contact area-trigger-entered
                                   :on-end-contact area-trigger-exited}))]
      (assoc component :body body))))


(defn- handle-begin-contact-event
  [system fixture-a fixture-b]
  (if-let [data (.getUserData fixture-a)]
    ((:on-begin-contact data) system (:entity data) fixture-b)
    system))

(defn- handle-end-contact-event
  [system fixture-a fixture-b]
  (if-let [data (.getUserData fixture-a)]
    ((:on-end-contact data) system (:entity data) fixture-b)
    system))

(defn- handle-begin-contact-events
  [system contact-events]
  (reduce (fn [system {:keys [fixture-a fixture-b]}]
            (-> system
                (handle-begin-contact-event fixture-a fixture-b)
                (handle-begin-contact-event fixture-b fixture-a)))
          system contact-events))

(defn- handle-end-contact-events
  [system contact-events]
  (reduce (fn [system {:keys [fixture-a fixture-b]}]
            (-> system
                (handle-end-contact-event fixture-a fixture-b)
                (handle-end-contact-event fixture-b fixture-a)))
          system contact-events))

(defn- handle-contact-events
  [system]

  (let [queued-begin-contact-events @begin-contact-events
        queued-end-contact-events @end-contact-events]
    ;; TODO how much risk of missing events is there here?
    (reset! begin-contact-events [])
    (reset! end-contact-events [])
    (-> system
        (handle-begin-contact-events queued-begin-contact-events)
        (handle-end-contact-events queued-end-contact-events))))

(defn on-shutdown
  "Dispose of Box2D world"
  [system]
  (println "Shutting down physics...")
  (if-let [world (get-in system [:physics :world])]
    (.dispose world)))
(s/defsubsystem physics

  :on-show
  (fn [system]
    (c/register-component-def 'AreaTrigger AreaTrigger)
    (c/register-component-def 'PhysicsBody PhysicsBody)
    (-> system
        (assoc-in [:physics :world] (create-world))
        (assoc-in [:physics :debug-renderer] (Box2DDebugRenderer.))
        (r/register-render-callback debug-render 2)))

  :on-shutdown on-shutdown

  :on-pre-render
  (fn [system]
    (if-let [world (get-in system [:physics :world])]
      (.step world (.getDeltaTime Gdx/graphics) 6 2)) ;; TODO - want fixed physics update
    (-> system
        (update-physics-bodies)
        (handle-contact-events))))

;; TODO - how much should we be cleaning up? (.dispose world) etc..
