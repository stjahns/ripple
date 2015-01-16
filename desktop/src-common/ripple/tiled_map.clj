(ns ripple.tiled-map
  (:require [ripple.assets :as a]
            [ripple.components :as c]
            [brute.entity :as e])
  (:import [com.badlogic.gdx.maps.tiled TmxMapLoader]
           [com.badlogic.gdx.maps MapLayer]
           [com.badlogic.gdx.maps.tiled.renderers OrthogonalTiledMapRenderer]))

;;
;; Representation of a LibGDX TiledMap
;;
(a/defasset tiled-map
  :create
  (fn [system {:keys [path]}]
    (-> (TmxMapLoader.)
        (.load path))))

(c/defcomponent OrthogonalTiledMapRenderer
  :create
  (fn [system {:keys [tiled-map unit]}]
    (let [tiled-map (a/get-asset system tiled-map)]
      {:renderer (OrthogonalTiledMapRenderer. tiled-map (float unit))})))

(defn- get-object-layers
  [tiled-map]
  (filter (fn [layer] (= (type layer) MapLayer))
   (.getLayers tiled-map)))

(defn- get-map-objects [tiled-map]
  (let [object-layer (first (get-object-layers tiled-map))]
    (.getObjects object-layer)))

(defn render-maps
  "Render any tiled map renderer components"
  [system]
  (doseq [entity (e/get-all-entities-with-component system 'OrthogonalTiledMapRenderer)]
    (let [tiled-map-component (e/get-component system entity 'OrthogonalTiledMapRenderer)
          tiled-map-renderer (:renderer tiled-map-component)
          camera (:camera (:renderer system))]
      (.setView tiled-map-renderer camera)
      (.render tiled-map-renderer)))
  system)

;; (defn- create-entity-from-map-object
;;   "For the given map object, create and add an entity with the required components
;;   (if appropriate) to the ES system"
;;   [system map-object]
;;   (let [ellipse (.getEllipse map-object)
;;         x (.x ellipse)
;;         y (.y ellipse)]
;;     (if (= (.getName map-object) "PlayerSpawn")
;;       nil ;;(create-player system x y)
;;       system)))

;; (defn- create-entities-in-map
;;   [system tiled-map]
;;   (let [map-objects (get-map-objects tiled-map)]
;;     (reduce (fn [system map-object]
;;               (create-entity-from-map-object system map-object))
;;             system map-objects)))
