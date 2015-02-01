(ns ripple.audio
  (:import [com.badlogic.gdx Gdx])
  (:require [ripple.subsystem :as s]
            [ripple.components :as c]
            [brute.entity :as e]
            [ripple.assets :as a]))

(def instances (atom []))

(a/defasset sound
  :create
  (fn [system {:keys [path]}]
    (let [instance (-> (Gdx/audio)
                       (.newSound (-> (Gdx/files)
                                      (.internal path))))]
      (swap! instances #(conj % instance))
      instance)))

(a/defasset music
  :create
  (fn [system {:keys [path]}]
    (let [instance (-> (Gdx/audio)
                       (.newMusic (-> (Gdx/files)
                                      (.internal path))))]
      (swap! instances #(conj % instance))
      instance)))

(c/defcomponent MusicPlayer
  :fields [:audio-asset {:asset true}
           :play-on-start {:default false}
           :looping {:default true}
           :pan {:default 0}
           :volume {:default 1}]
  :init (fn [component entity system params]
          (doto (:audio-asset component)
            (.setLooping (:looping component))
            (.setPan (:pan component) (:volume component))
            (.play))
          component))

(defn play-sound
  [system entity]
  (let [sound-player (e/get-component system entity 'SoundPlayer)
        {:keys [audio-asset looping volume pitch pan]} sound-player]
    (.play audio-asset volume pitch pan))
  system)

(defn on-shutdown
  "Stop all sounds and release all resources"
  [system]
  (println "Shutting down audio...")
  (doseq [instance @instances]
    (.stop instance)
    (.dispose instance))
  (reset! instances []))

(c/defcomponent SoundPlayer
  :on-event [:play play-sound]
  :fields [:audio-asset {:asset true}
           :play-on-start {:default false}
           :looping {:default true}
           :pan {:default 0}
           :pitch {:default 1}
           :volume {:default 1}]
  :init (fn [component entity system params]
          (assoc component :params params)))

(s/defsubsystem audio
  :on-show (fn [system]
             (a/register-asset-def :music music-asset-def)
             (a/register-asset-def :sound sound-asset-def)
             (c/register-component-def 'SoundPlayer SoundPlayer)
             (c/register-component-def 'MusicPlayer MusicPlayer)
             system)
  :on-shutdown on-shutdown)
