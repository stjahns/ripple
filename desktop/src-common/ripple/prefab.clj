(ns ripple.prefab
  (:require
   [brute.entity :as e]
   [ripple.assets :as asset-db]
   [ripple.components :as components]))

(defn override-prefab-params [asset options]
  (let [components (:components asset)]
    (assoc asset :components (map (fn [component]
                                    (let [component-type (keyword (clojure.string/lower-case (:type component)))
                                          component-options (get options component-type)]
                                      (assoc component :params (merge (:params component)
                                                                      component-options))))
                                  components))))

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

(defn instantiate [system asset-name options]
  "Get a prefab by name and instantiate it in the ES system"
  (let [asset-db (:asset-db system)
        asset (get asset-db asset-name)
        inst-fn (-> (symbol (:asset asset))
                    (asset-db/get-asset-def)
                    (get :instantiate))]
    (inst-fn system (override-prefab-params asset options))))
