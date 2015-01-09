(ns dungeon-sandbox.asset-database
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion Sprite Animation]
           [com.badlogic.gdx.graphics Texture]
           [com.badlogic.gdx.maps MapLayer]
           [com.badlogic.gdx.maps.tiled TmxMapLoader]
           [dungeon_sandbox.components Position SpriteRenderer TiledMapRendererComponent Player]
           )
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [dungeon-sandbox.move-target :as move-target]
            [dungeon-sandbox.player :as player]
            [dungeon-sandbox.rendering :as rendering]
            [dungeon-sandbox.components :as c]
            [dungeon-sandbox.input :as input]
            [clj-yaml.core :as yaml]
            [brute.entity :as e]
            [brute.system :as s]))



;; TODO - memoize for shared assets somehow...
(defn get-asset [system asset-name]
  "Get an asset by name and instantiate it"
  (let [asset-db (:asset-db system)
        asset (get asset-db asset-name)
        create-fn (ns-resolve 'dungeon-sandbox.asset-database
                              (symbol (str (:asset asset) "-create")))]
    (create-fn asset-db asset)))

(defmacro defasset
  [n & options]
  `(->> (for [[k# v#] ~(apply hash-map options)]
          [k# (intern *ns* (symbol (str '~n "-" (name k#))) v#)])
        (into {})))

;; Core asset definitions

(defasset texture
  :create
  (fn [system params]
    (Texture. (:path params))))

;;
;; Animation
;; texture -
;; tile-width -
;; tile-height -
;; frame-speed -
;; frames - vector of 2 element vectors corresponding to tile indices
;;
(defasset animation
  :create
  (fn [system params]
    ;; create a LibGDX animation using the asset params
    (let [frame-duration (:frame-speed params)
          frame-width (:tile-width params)
          frame-height (:tile-height params)
          texture (get-asset system (:texture params))
          key-frames (map #(TextureRegion. texture %1 %2 frame-width frame-height)
                          (:frames params))]
      (Animation. frame-duration key-frames))))

;; DUMB PLAN
;; Read every .yaml file in the resources directory tree

(defn- load-map-file
  "Return a TiledMap instance for the given path"
  [path]
  (let [map-loader (TmxMapLoader.)]
   (.load map-loader path)))

;; probably move to a 'level' module?

(defn- get-object-layers
  [tiled-map]
  (filter (fn [layer] (= (type layer) MapLayer))
   (.getLayers tiled-map)))

(defn- get-map-objects [tiled-map]
  (let [object-layer (first (get-object-layers tiled-map))]
    (.getObjects object-layer)))

;; TODO howto nice error checking, if-let?


(defn create-component [type params]
  "Given a component type and some params, instantiate the component data"
  (let [record-constructor (-> (apply str ["c/->" type]) ;; ghettoooo
                               (symbol)
                               (resolve))]
    (apply record-constructor params)))

(defn create-entity-from-prefab [system prefab-name params]
  (let [prefab (-> (:asset-db system)
                      (get prefab-name))
           entity (e/create-entity system)
           components (:components prefab)
           system-with-entity (e/add-entity system entity)]
    (reduce (fn [system component] (e/add-component system component))
            system components)
    system))

(defn- create-entity-from-map-object
  "For the given map object, create and add an entity with the required components
  (if appropriate) to the ES system"
  [system map-object]
  (let [ellipse (.getEllipse map-object)
        x (.x ellipse)
        y (.y ellipse)]
    (if (= (.getName map-object) "PlayerSpawn")
      nil ;;(create-player system x y)
      system)))


(defn- create-entities-in-map
  [system tiled-map]
  (let [map-objects (get-map-objects tiled-map)]
    (reduce (fn [system map-object]
              (create-entity-from-map-object system map-object))
            system map-objects)))

(defn- load-asset-file [path]
  (yaml/parse-string
   (slurp path)))

(defn init-asset-db [] {})

(defn load-asset [asset-db asset]
  (assoc asset-db (:name asset) asset))

(defn start
  "Start this system"
  [system]
  (let [asset-db (init-asset-db)
        parsed-assets (load-asset-file "resources/animations.yaml")]
    (assoc system :asset-db (reduce load-asset asset-db parsed-assets))))
