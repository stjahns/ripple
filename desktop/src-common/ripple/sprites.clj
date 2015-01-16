(ns ripple.sprites
  (require [ripple.subsystem :as s]
           [ripple.components :as c]
           [brute.entity :as e]
           [ripple.asset-database :as a])
  (import [com.badlogic.gdx.graphics.g2d SpriteBatch]))

(c/defcomponent SpriteRenderer
  :create
  (fn [system params]
    {:texture (a/get-asset system (:texture params))}))

(c/defcomponent AnimationController
  :create
  (fn [system params]
    {:animation nil
     :playing false
     :start-time nil}))

(defn play-animation [system entity animation]
  (e/update-component system entity 'AnimationController #(-> % (assoc :animation animation
                                                                       :start-time (/ (com.badlogic.gdx.utils.TimeUtils/millis) 1000.)
                                                                       :playing true))))

(defn- render-sprites
  "Render sprites for each SpriteRenderer component"
  [system]
  (let [sprite-batch (get-in system [:sprites :sprite-batch])
        camera (get-in system [:renderer :camera])
        pixels-per-unit (get-in system [:renderer :pixels-per-unit])]
    (.setProjectionMatrix sprite-batch (.combined camera))
    (.begin sprite-batch)
    (doseq [entity (e/get-all-entities-with-component system 'SpriteRenderer)]
      (let [sprite-renderer (e/get-component system entity 'SpriteRenderer)
            position (e/get-component system entity 'Position)
            texture (:texture sprite-renderer)
            x (float (/ (:x position) pixels-per-unit))
            y (float (/ (:y position) pixels-per-unit))]
        (.draw sprite-batch texture x y (float 1) (float 1))))
    (.end sprite-batch)
    system))

;;
;; Not a huge fan of this, would be nicer if you could somehow 'plug in' an AnimationController to a SpriteRenderer
;; so the SpriteRenderer can just query the controller for a texture region as necessary
;;
(defn- update-animation-controller
  [system entity]
  (let [animation-controller (e/get-component system entity 'AnimationController)
        sprite-renderer (e/get-component system entity 'SpriteRenderer)]
    (if-let [animation (:animation animation-controller)]
      (let [anim-time (- (/ (com.badlogic.gdx.utils.TimeUtils/millis) 1000.)
                         (:start-time animation-controller))
            anim-frame (.getKeyFrame animation (float anim-time) true)]
        (e/update-component system entity 'SpriteRenderer #(-> % (assoc :texture anim-frame))))
      system)))

(defn- update-animation-controllers
  [system]
  (let [entities (e/get-all-entities-with-component system 'AnimationController)]
    (reduce update-animation-controller
            system entities)))

(s/defsubsystem sprites

  :on-show
  (fn [system]
    (assoc-in system [:sprites :sprite-batch] (SpriteBatch.)))

  :on-render
  (fn [system]
    (-> system
        (update-animation-controllers)
        (render-sprites))))
