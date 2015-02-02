(ns ripple.prefab
  (:require
   [brute.entity :as e]
   [ripple.assets :as a]
   [ripple.subsystem :as s]
   [ripple.components :as components]))

(defn override-prefab-params [instance-def options]
  (let [components (:components instance-def)]
    (assoc instance-def :components (map (fn [component]
                                    (let [component-type (keyword (clojure.string/lower-case (:type component)))
                                          component-options (get options component-type)]
                                      (assoc component :params (merge (:params component)
                                                                      component-options))))
                                  components))))

(defn- instantiate-children
  "Given a list of children, instantiate them into the system"
  [system entity children]
  (reduce (fn [system {:keys [prefab options]}]
            (instantiate system prefab (assoc-in options [:transform :parent] entity)))
          system children))

(defn- add-components
  [system entity components]
  (reduce (fn [system component] (e/add-component system entity component))
          system components))

(a/defasset prefab
  :instantiate
  (fn [system params]
    (let [entity (e/create-entity)
          system (e/add-entity system entity)
          components (map #(components/create-component system
                                                        entity
                                                        (symbol (:type %))
                                                        (:params %))
                          (:components params))]
      (-> system
          (instantiate-children entity (:children params))
          (add-components entity components)))))

(defn instantiate [system asset-name options]
  "Get a prefab by name and instantiate it in the ES system"
  (let [instance-def (get-in system [:assets :instance-defs asset-name])
        asset-def (a/get-asset-def (keyword (:asset instance-def)))
        inst-fn (:instantiate asset-def)]
    (inst-fn system (override-prefab-params instance-def options))))

(s/defsubsystem prefabs
  :asset-defs [:prefab]
  :on-show (fn [system]
             (a/register-asset-def :prefab prefab-asset-def)
             system))
