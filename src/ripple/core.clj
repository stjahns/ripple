(ns ripple.core
  (:use [pallet.thread-expr])
  (:import [com.badlogic.gdx ApplicationListener])
  (:require [ripple.components :as c]
            [ripple.assets :as a]
            [ripple.subsystem :as subsystem]
            [brute.entity :as e]))

(def sys (atom 0))

(defn initialize
  "Initialize the ES system and all subsystems"
  [subsystems asset-sources on-initialized]
  (-> (e/create-system)
      (a/load-asset-instance-defs asset-sources)
      (for-> [subsystem subsystems]
             (subsystem/register-subsystem subsystem))
      (subsystem/on-system-event :on-show)
      (on-initialized)))
