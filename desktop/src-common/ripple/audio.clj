(ns ripple.audio
  (:import [com.badlogic.gdx Gdx])
  (:require [ripple.subsystem :as s]
            [ripple.components :as c]
            [ripple.assets :as a]))

(a/defasset sound
  :create
  (fn [system {:keys [path]}]
    (-> (Gdx/audio)
        (.newSound (-> (Gdx/files)
                       (.internal path))))))

(a/defasset music
  :create
  (fn [system {:keys [path]}]
    (-> (Gdx/audio)
        (.newMusic (-> (Gdx/files)
                       (.internal path))))))

;; FIXME -- need to stop all audio players when we reload....

(c/defcomponent AudioPlayer
  :fields [:audio-asset {:asset true}
           :looping {:default true}
           :pan {:default 0}
           :volume {:default 1}]
  :init (fn [component system params]
          (doto (:audio-asset component)
            (.setLooping (:looping component))
            (.setPan (:pan component) (:volume component))
            (.play))
          component))
