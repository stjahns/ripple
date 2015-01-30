(ns ripple.repl
  (:use aprint.core)
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.utils :as u]
            [ripple.core :refer :all]
            [ripple.player :as player]
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
            [brute.system :as s]
            [clojure.tools.namespace.repl :refer [refresh]]))

(defn contextual-eval [ctx expr]
    (eval
        `(let [~@(mapcat (fn [[k v]] [k `'~v]) ctx)]
             ~expr)))
(defmacro local-context []
    (let [symbols (keys &env)]
        (zipmap (map (fn [sym] `(quote ~sym)) symbols) symbols)))
(defn readr [prompt exit-code]
    (let [input (clojure.main/repl-read prompt exit-code)]
        (if (= input ::tl)
            exit-code
             input)))
;;make a break point
(defmacro break []
  `(clojure.main/repl
    :prompt #(print "debug=> ")
    :read readr
    :eval (partial contextual-eval (local-context))))

;; Exception Wrapper
(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!)))

(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                         (catch Exception e
                           (.printStackTrace e)
                           (set-screen! ripple blank-screen)))))

(defn shutdown []
  (println "Shutting down...")
  (set-screen! ripple blank-screen)
  (subsystem/on-system-event @sys :on-shutdown))

(defn reload-and-require-all []
  (shutdown)

  (println "Recompiling...")

  (require 'ripple.subsystem :reload-all)
  (require 'ripple.core :reload-all)
  (require 'ripple.core.desktop-launcher :reload-all)

  (println "Reloading...")
  (on-gl (set-screen! ripple main-screen)))

(defn reload-all []
  (shutdown)
  (on-gl (set-screen! ripple main-screen)))

(defn rra [] (reload-and-require-all))
(defn ra [] (reload-all))

(defn aps []
  (aprint (:asset-db @sys))
  (aprint (:physics @sys))
  (aprint (:renderer @sys)))

(defn apc []
  (aprint (:entity-components @sys)))

(defn p-subsystem-hooks []
  (aprint @ripple.subsystem/subsystems))

(defn p-asset-defs []
  (aprint @ripple.assets/asset-defs))
