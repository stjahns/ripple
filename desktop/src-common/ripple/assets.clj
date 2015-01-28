(ns ripple.assets
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion Animation]
           [com.badlogic.gdx.graphics Texture])
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.subsystem :as s]
            [clj-yaml.core :as yaml]
            [brute.entity :as e]))

(defonce asset-defs (atom {}))

(defn get-asset-def [asset-symbol]
  (get @asset-defs asset-symbol))

(defn remove-asset-def [asset-symbol]
  (swap! asset-defs dissoc asset-symbol))

(defn register-asset-def [asset-symbol create-fn]
  (swap! asset-defs assoc asset-symbol create-fn))

(defn get-asset [system asset-name]
  "Get an asset by name and instantiate it"
  (let [asset-db (:asset-db system)
        asset (get asset-db asset-name)
        asset-def (-> (symbol (:asset asset))
                      (get-asset-def))
        create-fn (:create asset-def)]
    (when (not asset-def) (throw (Exception. (str "Asset not defined: " (:asset asset)))))
    (create-fn system asset)))

(defmacro defasset
  [n & options]
  `(let [options# ~(apply hash-map options)]
     (register-asset-def '~n options#)))

;; Core asset definitions

(defasset texture
  :create
  (fn [system {:keys [path]}]
    (Texture. path)))

(defasset texture-region
  :create
  (fn [system {texture-id :texture
               [x y] :tile-indices
               [width height] :tile-size}]
    (let [texture (get-asset system texture-id)]
      (TextureRegion. texture x y width height))))

(defasset animation
  :create
  (fn [system {:keys [frame-duration
                      texture
                      frames] ; frames should be a list of [x, y] pairs
               [frame-width frame-height] :frame-size}]
    (let [texture (get-asset system texture)
          key-frames (map #(TextureRegion. texture
                                           (* frame-width (first %))
                                           (* frame-height (second %))
                                           frame-width
                                           frame-height)
                          frames)]
      (Animation. (float frame-duration)
                  (u/gdx-array key-frames)))))

(defn- load-asset-file [path]
  (yaml/parse-string
   (slurp path)))

(defn init-asset-db [] {})

(defn load-asset [asset-db asset]
  (assoc asset-db (:name asset) asset))

(s/defsubsystem asset-db
  :on-show
  (fn [system]
    (let [asset-db (init-asset-db)
          asset-files ["resources/assets.yaml"]
          parsed-assets (flatten (map load-asset-file asset-files))]
      (assoc system :asset-db (reduce load-asset asset-db parsed-assets)))))
