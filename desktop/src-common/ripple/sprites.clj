(ns ripple.sprites
  (require [ripple.subsystem :as s]
           [ripple.components :as c]
           [ripple.rendering :as r]
           [ripple.assets :as a]
           [ play-clj.utils :as u]
           [brute.entity :as e])
  (import [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion Animation BitmapFont]
          [com.badlogic.gdx.graphics Texture Color]
          [com.badlogic.gdx Gdx]))

(a/defasset texture
  :create
  (fn [system {:keys [path]}]
    (or (u/load-asset path Texture)
        (Texture. path))))

(a/defasset texture-region
  :create
  (fn [system {texture-id :texture
               [x y] :tile-indices
               [width height] :tile-size}]
    (let [texture (a/get-asset system texture-id)]
      (TextureRegion. texture x y width height))))

(a/defasset animation
  :create
  (fn [system {:keys [frame-duration
                      texture
                      frames] ; frames should be a list of [x, y] pairs
               [frame-width frame-height] :frame-size}]
    (let [texture (a/get-asset system texture)
          key-frames (map #(TextureRegion. texture
                                           (* frame-width (first %))
                                           (* frame-height (second %))
                                           frame-width
                                           frame-height)
                          frames)]
      (Animation. (float frame-duration)
                  (u/gdx-array key-frames)))))

(c/defcomponent SpriteRenderer
  :fields [:texture {:asset true}
           :flip-x {:default false}])

(defn- text-renderer-show-text
  [system entity event]
  (e/update-component system entity 'TextRenderer #(assoc % :text (:text event))))

(c/defcomponent TextRenderer
  :on-event [:show-text text-renderer-show-text]
  :fields [:text {:default "TEXT"}
           :position {:default [100 100]}]
  :init
  (fn [component entity system {:keys [font]}]
    (assoc component :font (BitmapFont.))))

(c/defcomponent AnimationController
  :fields [:animation {:asset true}
           :playing {:default false}
           :play-on-start {:default false}
           :start-time {:default 0}])

(defn play-animation [system entity animation]
  (e/update-component system entity 'AnimationController #(-> % (assoc :animation animation
                                                                       :start-time (/ (com.badlogic.gdx.utils.TimeUtils/millis) 1000.)
                                                                       :playing true))))

(defmulti get-sprite-size class)

(defmethod get-sprite-size com.badlogic.gdx.graphics.Texture
  [texture]
  [(.getWidth texture) (.getHeight texture)])

(defmethod get-sprite-size com.badlogic.gdx.graphics.g2d.TextureRegion
  [texture]
  [(.getRegionWidth texture) (.getRegionHeight texture)])

;; TODO - maybe it would simplify things if we convert everything to TextureRegions

(defmulti draw-sprite (fn [sprite-batch texture & args] (class texture)))

(defmethod draw-sprite com.badlogic.gdx.graphics.g2d.TextureRegion
  [sprite-batch texture [x y] [width height] [scale-x scale-y] rotation flip-x]
  
  ;; ew
  (when (not (= (.isFlipX texture) flip-x))
    (.flip texture true false))

  (.draw sprite-batch texture
         (float (- x (/ width 2))) (float (- y (/ height 2)))
         (/ width 2) (/ height 2)
         (float width) (float height)
         scale-x scale-y
         rotation))

(defmethod draw-sprite com.badlogic.gdx.graphics.Texture
  [sprite-batch texture [x y] [width height] [scale-x scale-y] rotation flip-x]
  ;; TODO handle flip-x ....
  (let [[srcWidth srcHeight] (get-sprite-size texture)]
    (.draw sprite-batch texture
           (float (- x (/ width 2))) (float (- y (/ height 2)))
           (/ width 2) (/ height 2)
           (float width) (float height)
           (float scale-x) (float scale-y)
           (float rotation)
           0 0
           srcWidth srcHeight
           false false)))

(defn- render-text-renderers
  "Render text for each TextRenderer component"
  [system]
  (let [sprite-batch (get-in system [:sprites :sprite-batch])
        screen-matrix (get-in system [:renderer :screen-matrix])]
    ;; BitmapFont needs to work in screen space
    (.setProjectionMatrix sprite-batch screen-matrix)
    (.begin sprite-batch)
    (doseq [entity (e/get-all-entities-with-component system 'TextRenderer)]
      (let [text-renderer (e/get-component system entity 'TextRenderer)
            bitmap-font (:font text-renderer)
            text (:text text-renderer) 
            [x y] (:position text-renderer)]
        (.draw bitmap-font sprite-batch text x y)))
    (.end sprite-batch)
    system))

(defn- render-sprites
  "Render sprites for each SpriteRenderer component"
  [system]
  (let [sprite-batch (get-in system [:sprites :sprite-batch])
        camera (get-in system [:renderer :camera])]
    (.setProjectionMatrix sprite-batch (.combined camera))
    (.begin sprite-batch)
    (doseq [entity (e/get-all-entities-with-component system 'SpriteRenderer)]
      (let [sprite-renderer (e/get-component system entity 'SpriteRenderer)
            transform (e/get-component system entity 'Transform)

            position (c/get-position system transform)
            scale (c/get-scale system transform)
            r (c/get-rotation system transform)

            pixels-per-unit (get-in system [:renderer :pixels-per-unit])
            texture (:texture sprite-renderer)
            [width height] (map #(/ % pixels-per-unit)
                                (get-sprite-size texture))]
        ; TODO handle color?
        ;(.setColor sprite-batch Color/RED)
        (draw-sprite sprite-batch
                     texture
                     [(.x position) (.y position)]
                     [width height]
                     [(.x scale) (.y scale)]
                     r
                     (:flip-x sprite-renderer))))
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

(defn- autostart-animation
  [system entity]
  (let [animation-controller (e/get-component system entity 'AnimationController)]
    (if (and (:play-on-start animation-controller)
             (not (:playing animation-controller)))
      (play-animation system entity (:animation animation-controller))
      system)))

(defn- autostart-animations
  [system]
  (let [entities (e/get-all-entities-with-component system 'AnimationController)]
    (reduce autostart-animation
            system entities)))

(s/defsubsystem sprites

  :on-show
  (fn [system]
    (a/register-asset-def :texture texture-asset-def)
    (a/register-asset-def :texture-region texture-region-asset-def)
    (a/register-asset-def :animation animation-asset-def)
    (c/register-component-def 'SpriteRenderer SpriteRenderer)
    (c/register-component-def 'TextRenderer TextRenderer)
    (c/register-component-def 'AnimationController AnimationController)
    (-> system
        (assoc-in [:sprites :sprite-batch] (SpriteBatch.))
        (r/register-render-callback render-sprites 1)
        (r/register-render-callback render-text-renderers 3))) ;; TODO - be able to specify order for each SpriteRenderer component

  :on-pre-render
  (fn [system]
    (-> system
        (autostart-animations)
        (update-animation-controllers))))
