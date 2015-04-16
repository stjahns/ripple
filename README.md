## Ripple

A Clojure framework for 2D game development, piecing together [play-clj](https://github.com/oakes/play-clj),  [Brute](https://github.com/markmandel/brute) and [LibGDX](https://github.com/libGDX/libGDX)

Example projects: [Space Roach Exterminator II](https://github.com/stjahns/space-roach-exterminator-II), [Leaks!](https://github.com/stjahns/leaks)

## Usage

With Leiningen, create a new app project:

```
lein new app my_game
```  

Add the following to `:dependencies` in your `project.clj`:

```Clojure
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "1.1.0"]
                 [ripple "0.1.0-SNAPSHOT"]]
```

Create a `checkouts` directory in your project, and clone the Ripple repository into it. This will allow you to easily make changes to Ripple itself while working in your own project.

```
cd ~/my_game
mkdir checkouts
cd checkouts
git clone https://github.com/stjahns/ripple.git
```

For now, you can copy the following template into your `core.clj`:

```Clojure
(ns my-game.core
  (:require [play-clj.core :refer :all]
            [ripple.repl :as repl]
            [ripple.assets :as a]
            [ripple.audio :as audio]
            [ripple.components :as c]
            [ripple.core :as ripple]
            [ripple.event :as event]
            [ripple.physics :as physics]
            [ripple.prefab :as prefab]
            [ripple.rendering :as rendering]
            [ripple.sprites :as sprites]
            [ripple.subsystem :as subsystem]
            [ripple.tiled-map :as tiled-map]
            [ripple.transform :as transform])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(declare shutdown restart)

;; This declares all the builtin Ripple subsystems that should be used, as well as any additional subsystems you define
(def subsystems [transform/transform
                 event/events
                 rendering/rendering
                 physics/physics
                 prefab/prefabs
                 sprites/sprites
                 audio/audio
                 tiled-map/level
                 ;; Add your own subsystems here
                 ])

;; Create a file called "assets.yaml" under the project's 'resources' directory
(def asset-sources ["assets.yaml"])

(defn on-initialized
  "Ripple is loaded. Load your level or whatever"
  [system]
  ;; For example:
  ;; (-> system (prefab/instantiate "ShipLevel" {}))
  system)

;; This comes from `play-clj`, defines our LibGDX Screen
;; We ignore `screen` and `entities` as we are using our own entity system based on Brute
(defscreen main-screen
  :on-show
  (fn [screen entities]

    ;; Initialize Ripple
    (reset! ripple/sys (-> (ripple/initialize subsystems asset-sources on-initialized)
                           (assoc-in [:renderer :clear-color] [0.2 0.2 0.2 1.0])))

    ;; Use an orthographic camera
    (update! screen :renderer (stage) :camera (orthographic))

    nil)

  :on-touch-down
  (fn [screen entities]
    (reset! ripple/sys (-> @ripple/sys (subsystem/on-system-event :on-touch-down)))
    nil)

  :on-render
  (fn [screen entities]
    (reset! ripple/sys (-> @ripple/sys
                    (subsystem/on-system-event :on-pre-render)
                    (subsystem/on-system-event :on-render)))
    (when (:restart @ripple/sys)
      (shutdown)
      (restart))
    nil)

  :on-resize
  (fn [screen entities]
    (reset! ripple/sys (-> @ripple/sys (subsystem/on-system-event :on-resize)))
    nil))

(defgame my-game
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

(defn -main []
  (LwjglApplication. my-game "My Ripple Game" 800 600)
  (Keyboard/enableRepeatEvents true))

;; For development, these are some convenient bits for releoading the game
;; after an exception, or when you want to reload your level after making
;; changes to code or data

(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!)))

(defn shutdown []
  (set-screen! my-game blank-screen)
  (Thread/sleep 100)
  (subsystem/on-system-event @ripple/sys :on-shutdown))

(defn restart []
  (set-screen! my-game main-screen))

;; For exception handling...
(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                         (catch Exception e
                           (.printStackTrace e)
                           (set-screen! my-game blank-screen)))))

(defn reload-all []
  (shutdown)
  (on-gl (set-screen! my-game main-screen)))

(defn reload-and-require-all []
  (shutdown)

  (println "Recompiling...")

  (require 'my-game.core :reload-all)

  (println "Reloading...")

  (on-gl (set-screen! my-game main-screen)))

(defn rra [] (reload-and-require-all))
(defn ra [] (reload-all))
```

