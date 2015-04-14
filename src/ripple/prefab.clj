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
                                      (merge component component-options)))
                                  components))))

(defn instantiate 
  "Get a prefab by name and instantiate it in the ES system"
  [system asset-name options]
  (let [instance-def (get-in system [:assets :instance-defs asset-name])
        asset-def (a/get-asset-def system (keyword (:asset instance-def)))
        inst-fn (:instantiate asset-def)]
    (inst-fn system (override-prefab-params instance-def options))))

(defn- instantiate-children
  "Given a list of children, instantiate them into the system"
  [system parent-entity children]
  (reduce (fn [system child-params]
            (instantiate system 
                         (:prefab child-params) 
                         (assoc-in child-params [:transform :parent] parent-entity)))
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
                                                        %)
                          (:components params))]
      (-> system
          (instantiate-children entity (:children params))
          (add-components entity components)))))

(s/defsubsystem prefabs
  :asset-defs [:prefab])
