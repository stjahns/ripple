(ns ripple.audio
  (:import [com.badlogic.gdx Gdx])
  (:require [ripple.subsystem :as s]
            [ripple.components :as c]
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

(c/defcomponent SoundPlayer
  :fields [:audio-asset {:asset true}
           :play-on-start {:default false}
           :looping {:default true}
           :pan {:default 0}
           :volume {:default 1}]
  :init (fn [component entity system params]
          (assoc component :params params)))

(defn on-shutdown
  "Stop all sounds and release all resources"
  [system]
  (println "Shutting down audio...")
  (doseq [instance @instances]
    (.stop instance)
    (.dispose instance))
  (reset! instances []))

(s/defsubsystem audio
  :on-show (fn [system]
             (a/register-asset-def :music music-asset-def)
             (a/register-asset-def :sound sound-asset-def)
             (c/register-component-def 'SoundPlayer SoundPlayer)
             (c/register-component-def 'MusicPlayer MusicPlayer)
             system)
  :on-shutdown on-shutdown)
