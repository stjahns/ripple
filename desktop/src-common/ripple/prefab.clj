(ns ripple.prefab
  (:require
   [brute.entity :as e]
   [ripple.asset-database :as asset-db]
   [ripple.components :as components]))

(asset-db/defasset prefab
  :instantiate
  (fn [system params]
    (let [entity (e/create-entity)
          system (e/add-entity system entity)
          components (map #(components/create-component system
                                                        (symbol (:type %))
                                                        (:params %))
                          (:components params))]
      (reduce (fn [system component] (e/add-component system entity component))
              system components))))

;; Should this go in asset?

(defn instantiate [system asset-name options]
  "Get a prefab by name and instantiate it in the ES system"
  (let [asset-db (:asset-db system)
        asset (get asset-db asset-name)
        inst-fn (-> (symbol (:asset asset))
                    (asset-db/get-asset-def)
                    (:instantiate))]
    (inst-fn system asset)))
