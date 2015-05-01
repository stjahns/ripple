## Ripple

A Clojure framework for 2D game development, piecing together [play-clj](https://github.com/oakes/play-clj),  [Brute](https://github.com/markmandel/brute) and [LibGDX](https://github.com/libGDX/libGDX)

Example projects: [Space Roach Exterminator II](https://github.com/stjahns/space-roach-exterminator-II), [Leaks!](https://github.com/stjahns/leaks)

## Usage

With Leiningen, create a new app project:

```
$ lein new app my_game
```  

Add the following to `:dependencies` in your `project.clj`:

```Clojure
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "1.1.0"]
                 [ripple "0.1.0-SNAPSHOT"]]
```

Create a `checkouts` directory in your project, and clone the Ripple repository into it. This will allow you to easily make changes to Ripple itself while working in your own project.

```
$ cd ~/my_game
$ mkdir checkouts
$ cd checkouts
$ git clone https://github.com/stjahns/ripple.git
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

;; This declares all the builtin Ripple subsystems that should be used, as well
;; as any additional subsystems you define
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
  "Ripple is loaded. Load your level or player or whatever"
  [system]
  ;; For example:
  ;; (-> system (prefab/instantiate "PlayerPrefab" {}))
  system)

;; This comes from `play-clj`, defines our LibGDX Screen
;; We ignore `screen` and `entities` as we are using our own entity system
;; based on Brute
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

## Running

This template should give you enough to bring up an empty grey screen with 

```
$ lein run
```

However, development is a lot more fun when running in a REPL:

```
$ lein repl
my-game.core=> (-main)
```            

This should start the game, leaving you with additionaly with a live REPL console to play with. For example, you can reload the level after making changes to data with:

```
my-game.core=> (ra)
```

Or, you can additionally reload any changes to code with:

```
my-game.core=> (rra)
```

## Adding Subsystems

In Ripple, any additional assets, components, or any other systems you define are added into modules called 'subsystems'. For example, you could define a 'level' subsystem that includes a 'Player' component with the following:

```Clojure
(defn update-player
  "If space is pressed, play jump-sound and bump up our position 10 units"
  [system entity]
  (let [player (brute.entity/get-component system entity 'Player)
        [position-x position-y] (:position (brute.entity/get-component system entity 'Transform))]
     (if (.isKeyPressed com.badlogic.gdx.Gdx/input com.badlogic.gdx.Input$Keys/SPACE)
         (do (.play (:jump-sound player))
             (brute.entity/update-component system entity 'Transform #(assoc % :position [position-x (+ 10 position-y)])))
          system)))

(defn player-on-spawn
  "Play spawn-sound"
  [system entity event]
  (let [player (brute.entity/get-component system entity 'Player)]
       (.play (:spawn-sound player))
       system))

(c/defcomponent Player
  :fields [:jump-sound {:asset true}
           :spawn-sound {:asset true}]
  :on-pre-render update-player
  :on-event [:on-spawn player-on-spawn])

(s/defsubsystem level-systems
  :component-defs ['Player])
```

Then, we just add `level-systems` to the subsystems list that we pass to Ripple on initialization, eg:

```Clojure
(def subsystems [transform/transform
                 event/events
                 rendering/rendering
                 physics/physics
                 prefab/prefabs
                 sprites/sprites
                 audio/audio
                 tiled-map/level
                 ;; Add your own subsystems here
                 level-systems])
```

Subsystems can also define new asset types, or hook into a number of different events or callbacks. For example, see the built-in [`sprites` subsystem](https://github.com/stjahns/ripple/blob/master/src/ripple/sprites.clj#L260) or the built-in [physics subsystem](https://github.com/stjahns/ripple/blob/master/src/ripple/physics.clj#L296).

## Defining 'Asset Instances'

In `resources/assets.yaml`, we can put the following:

```yaml

  # 'texture' is an asset an defined in ripple.sprite
  - asset: texture
    name: PlayerSpriteSheet
    path: player.png # Path is relative to 'resources' directory in your project

  # 'animation' is also defined in ripple.sprite
  - asset: animation
    name: PlayerWalking
    texture: PlayerSpriteSheet
    frame-size: [32, 32] ;; The size of each frame of the animation
    frame-duration: 0.1 ;; How long each frame should be visible for in the animation sequence
    frames:
      # A list of frame coordinates for an animation that is 4 frames within
      # the spritesheet, arranged horizontally at the top
      - [0, 0]
      - [1, 0]
      - [2, 0]
      - [3, 0]

  # 'sound' is an asset defined in ripple.audio
  - asset: sound
    name: JumpSound
    path: jump.wav

  - asset: sound
    name: SpawnSound
    path: spawn.wav

  # 'prefab' is a special type of asset that defines a collection of components that can be
  # 'instantiated' into the game
  
  - asset: prefab
    name: PlayerPrefab
    components:
      - type: Transform # A component for position, scale and rotation
        x: 10.0 # We can specify component values here in the prefab definition, or leave
        y: 0.0  # them to their default values

      - type: EventHub # A component that contains an event queue which can recieve events
                       # that can be handled by other components on the entity

      - type: SpriteRenderer # Renders a sprite. Can be used with AnimationController to play a sprite animation

      - type: AnimationController
        animation: PlayerWalking
        play-on-start: true 

      - type: PhysicsBody # A component that adds physics simulation, with configurable collision!
        body-type: dynamic
        fixed-rotation: true
        fixtures:
          - shape: circle
            radius: 0.5
            density: 1
            friction: 0.7

      - type: Player # The component we defined above!
        jump-sound: JumpSound # we can point jump-sound and spawn-sound to the sound assets we defined
        spawn-sound: SpawnSound

```

Asset definitions can be split into multiple files, as long as each file is added to `asset-sources`. The order of definitions does not matter.
