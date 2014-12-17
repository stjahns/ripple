(ns ripple.core.desktop-launcher
  (:require [ripple.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. ripple "ripple" 800 600)
  (Keyboard/enableRepeatEvents true))
