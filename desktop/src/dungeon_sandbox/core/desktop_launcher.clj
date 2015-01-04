(ns dungeon-sandbox.core.desktop-launcher
  (:require [dungeon-sandbox.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. dungeon-sandbox "dungeon-sandbox" 800 600)
  (Keyboard/enableRepeatEvents true))
