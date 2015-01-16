(ns ripple.prefab-test
  (:use [clojure.test])
  (:require [ripple.prefab :as prefab]
            [brute.entity :as e]
            [ripple.assets :as asset-db]
            [ripple.components :as components]))

(deftest override-prefab-params
  (testing "should merge prefab params with options"
    (let [asset {:asset "prefab"
                 :name "prefab1"
                 :components [{:type "Player"
                               :params {:x 1
                                        :y 5}}
                              {:type "Stuff"
                               :params {:a 4
                                        :b 9}}]}
          options {:player {:x 25}
                   :stuff {:b 2}}]

      (is (= (prefab/override-prefab-params asset options)
             {:asset "prefab"
              :name "prefab1"
              :components [{:type "Player"
                            :params {:x 25
                                     :y 5}}
                           {:type "Stuff"
                            :params {:a 4
                                     :b 2}}]})))))

(deftest prefab

  (components/defcomponent TestComponent
    :create (fn [system params]
              {:some-field (* (:x params)
                              (:y params))
               :stuff (:stuff (asset-db/get-asset system (:stuff params)))}))

  (asset-db/defasset test-asset
    :create (fn [system params] params))

  ;; TODO - would be nice if we could pass some callback to instantiate, or get the new instance UUID back
  ;; along with the system...

  (testing "prefab instantiation"

    ;; TODO - test prefabs that refer to other assets

    (let [system (asset-db/start {} ["resources/test/test-prefab.yaml"])]
      (let [system-with-instance (prefab/instantiate system "TestPrefab" {})
            entity (first (e/get-all-entities system-with-instance))
            component (e/get-component system-with-instance entity 'TestComponent)]

        (is (not= entity nil))
        (is (not= component nil))
        (is (= (:some-field component) 15))
        (is (= (:stuff component) "thing"))))))
