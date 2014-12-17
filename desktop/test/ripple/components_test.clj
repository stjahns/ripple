(ns ripple.components-test
  (:use [clojure.test])
  (:require [ripple.components :as c]
            [brute.entity :as e]))

(deftest defcomponent

  (c/defcomponent TestComponent
    :create (fn [system params]
              params))

  (testing "should register the component definition"
    (is (not= (c/get-component-def 'TestComponent)
              nil))))

(deftest create-component

  (c/defcomponent TestComponent
    :create (fn [system params]
              {:some-field (* (:x params)
                              (:y params))}))

  (testing "should create component with the expected fields"
    (let [system {}
          component (c/create-component system 'TestComponent {:x 5 :y 3})]
      (is (= (:some-field component)
             15)))))
