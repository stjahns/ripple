(ns ripple.tiled-map
  (:require [])
  (:import []))

;; TiledMap related stuff
;; TODO

;; (c/defcomponent TiledMapRendererComponent
;;   :create
;;   (fn [system params]))

;; (defn create-tiled-map-component
;;   [path unit]
;;   (let [renderer (orthogonal-tiled-map path unit)]
;;     (assoc (c/->TiledMapRendererComponent renderer)
;;            :type 'TiledMapRendererComponent)))

;; (defn- render-maps
;;   "Render any tiled map renderer components"
;;   [system]
;;   (doseq [entity (e/get-all-entities-with-component system 'TiledMapRendererComponent)]
;;     (let [tiled-map-component (e/get-component system entity 'TiledMapRendererComponent)
;;           tiled-map-renderer (:tiled-map-renderer tiled-map-component)
;;           camera (:camera (:renderer system))]
;;       (.setView tiled-map-renderer camera)
;;       (.render tiled-map-renderer))))

;; (defn- load-map-file
;;   "Return a TiledMap instance for the given path"
;;   [path]
;;   (let [map-loader (TmxMapLoader.)]
;;    (.load map-loader path)))

;; ;; probably move to a 'level' module?

;; (defn- get-object-layers
;;   [tiled-map]
;;   (filter (fn [layer] (= (type layer) MapLayer))
;;    (.getLayers tiled-map)))

;; (defn- get-map-objects [tiled-map]
;;   (let [object-layer (first (get-object-layers tiled-map))]
;;     (.getObjects object-layer)))

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
