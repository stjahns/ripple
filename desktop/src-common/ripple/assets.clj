(ns ripple.assets
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion Animation]
           [com.badlogic.gdx.graphics Texture])
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.subsystem :as s]
            [clj-yaml.core :as yaml]
            [brute.entity :as e]))

;; Asset manager module
;;
;; Terminology:
;;
;; asset-def - Definition of a type of asset, eg a sprite, audio file, etc.
;; References a constructor fn to instantiate the asset given some options
;;
;; asset-instance-def - Definition of an instance of an asset, eg a sprite for "player.png"
;; that is parsed from an asset yaml file
;;
;; asset-instance - the created instance of a particular asset instance definition

(defonce asset-defs
  ;; Mapping of keywords to asset definitions
  ;; TODO keep in system..?
  (atom {}))

(defn get-asset-def
  "Returns the asset definition for the given key
  Asset definitions should be maps of the form {:create fn} or {:instantiate fn} depending on whether
  it should be instantiated into the system or not'" ;; TODO needlessly complicated?
  [key]
  (get @asset-defs key))

(defn register-asset-def
  "Register the given asset definition with the given key"
  [key asset-def]
  (swap! asset-defs assoc key asset-def))

(defn get-asset [system asset-name]
  "Get an asset instance definition by name and instantiate it"
  (let [instance-def (get-in system [:assets :instance-defs asset-name])
        asset-def (get-asset-def (keyword (:asset instance-def)))
        create-fn (:create asset-def)]
    (when (not asset-def) (throw (Exception. (str "Asset definition does not exist: " (:asset instance-def)))))
    (create-fn system instance-def)))

(defmacro defasset
  "Interns a symbol '{name}-asset-name in the current namespace bound to the
  enclosed asset definition. The asset def should be registered with asset manager
  during subsystem initialization"
  [n & options]
  `(let [options# ~(apply hash-map options)]
     (intern *ns* (symbol (str '~n "-asset-def")) options#)))

;; TODO move these to the proper modules...

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

(defn- parse-asset-file
  "Parse the asset source file for the given path"
  [path]
  (yaml/parse-string
   (slurp path)))

(defn load-asset-instance-defs
  "Parses asset source files into instance definitions
   and stores them in the system indexed by keyword corresponding to the asset instance name.
  This can happen at any time."
  [system]
  (let [asset-files ["resources/space_roaches/assets.yaml"]
        instance-defs (flatten (map parse-asset-file asset-files))]
    (assoc-in system [:assets :instance-defs]
              (reduce #(assoc % (:name %2) %2)
                      {} instance-defs))))

(defn init-asset-manager
  "Clear any old asset definitions"
  [system]
  (reset! asset-defs {})
  system)

(s/defsubsystem assets
  ;;:asset-defs [:texture :texture-region :animation] ;; TODO handle with macro
  :on-show
  (fn [system]
    (register-asset-def :texture texture-asset-def)
    (register-asset-def :texture-region texture-region-asset-def)
    (register-asset-def :animation animation-asset-def)
    (load-asset-instance-defs system)))
