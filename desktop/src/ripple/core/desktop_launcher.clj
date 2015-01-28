(ns ripple.core.desktop-launcher
  (:use aprint.core)
  (:require [ripple.core :refer :all]
            [ripple.repl :refer :all]
            )
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. ripple "ripple" 800 600)
  (Keyboard/enableRepeatEvents true))
