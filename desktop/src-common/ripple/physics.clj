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
            FixtureDef
            Box2DDebugRenderer]
           [com.badlogic.gdx.math Vector2]
           [com.badlogic.gdx Gdx]))

(c/defcomponent BoxFixture
  :create
  (fn [system {:keys [x y width height density body-type]}]
    (let [world (get-in system [:physics :world])
          body-type (case body-type
                      "dynamic" BodyDef$BodyType/DynamicBody
                      "kinematic" BodyDef$BodyType/KinematicBody
                      "static" BodyDef$BodyType/StaticBody)
          body-def (doto (BodyDef.)
                     (-> .type (set! body-type))
                     (-> .position (.set x y)))
          shape-def (doto (PolygonShape.)
                      (.setAsBox (/ width 2) (/ height 2))) ;;  setAsBox takes half-width and half-height
          fixture-def (doto (FixtureDef.)
                        (-> .shape (set! shape-def))
                        (-> .density (set! density)))
          body (.createBody world body-def)
          fixture (.createFixture body fixture-def)]
      {:body body
       :fixture fixture})))

(defn- create-world []
  (let [gravity (Vector2. 0 -98)
        do-sleep true]
    (World. gravity do-sleep)))

(defn- update-box-fixture
  "Updates the Position component on the entity with the current position of the Box2D body"
  [system entity]
  (let [body-position (-> (e/get-component system entity 'BoxFixture)
                          (:body)
                          (.getPosition))
        x (.x body-position)
        y (.y body-position)]
    (e/update-component system entity 'Position #(-> % (assoc :x x :y y)))))

(defn- update-physics-bodies
  [system]
  (let [entities (e/get-all-entities-with-component system 'BoxFixture)]
    (reduce update-box-fixture
            system entities)))

(defn- debug-render*
  [system]
  (let [debug-renderer (get-in system [:physics :debug-renderer])
        world (get-in system [:physics :world])
        camera (get-in system [:renderer :camera])
        pixels-per-unit (get-in system [:renderer :pixels-per-unit])
        projection-matrix (.scale (.cpy (.combined camera))
                                  (/ 1 pixels-per-unit) (/ 1 pixels-per-unit) 1)]
    (.render debug-renderer world projection-matrix))
  system)

(defn- debug-render [system] (debug-render* system) system)

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
        (update-physics-bodies))))

;; TODO - how much should we be cleaning up? (.dispose world) etc..
