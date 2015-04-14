(ns ripple.repl
  (:use aprint.core)
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.core :refer :all]
            [ripple.rendering :as rendering]
            [ripple.sprites :as sprites]
            [ripple.physics :as physics]
            [ripple.audio :as audio]
            [ripple.components :as c]
            [ripple.assets :as a]
            [ripple.subsystem :as subsystem]
            [ripple.prefab :as prefab]
            [ripple.tiled-map :as tiled-map]
            [brute.entity :as e]
            [brute.system :as s]))

;; (defn resume []
;;   (on-gl (set-screen! ripple main-screen)))

;; (defn reload-and-require-all []
;;   (shutdown)

;;   (println "Recompiling...")

;;   (require 'ripple.subsystem :reload-all)
;;   (require 'ripple.core :reload-all)
;;   (require 'ripple.core.desktop-launcher :reload-all)

;;   (println "Reloading...")
;;   (on-gl (set-screen! ripple main-screen)))

;; (defn reload-all []
;;   (shutdown)
;;   (on-gl (set-screen! ripple main-screen)))

;; (defn rra [] (reload-and-require-all))
;; (defn ra [] (reload-all))

(defn aps []
  (aprint (:assets @sys))
  (aprint (:physics @sys))
  (aprint (:renderer @sys)))

(defn apc []
  (aprint (:entity-components @sys)))

(defn get-components [system type]
  (map #(e/get-component system % type)
       (e/get-all-entities-with-component system type)))
