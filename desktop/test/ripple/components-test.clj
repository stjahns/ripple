(ns ripple.components-test
  (:use [clojure.test])
  (:require [ripple.components :as c]
            [brute.entity :as e]))

(deftest defcomponent

  ;; (c/defcomponent TestComponent [x y]
  ;;   :create (fn [component params]
  ;;             (assoc component
  ;;                    :x (:x params)
  ;;                    :y (:y params))))

  (c/defcomponent TestComponent
    :create (fn [system params]
              params))

  ;; (testing "should define a record in the current namespace"
  ;;   (is (not= (->TestComponent 0 0)
  ;;             nil)))

  (testing "should register the component definition"
    (is (not= (c/get-component-def 'TestComponent)
              nil)))

  ;; (testing "should map the component symbol to the options given to defcomponent"
  ;;   (is (not= (c/get-component-def 'TestComponent)
  ;;             nil))
  ;;   (is (= (:component (c/get-component-def 'TestComponent))
  ;;          TestComponent))))
  )

(deftest create-component

  (c/defcomponent TestComponent
    :create (fn [params]
              {:some-field (* (:x params)
                              (:y params))})))

  ;; (c/defcomponent TestComponent [x y]
  ;;   :create (fn [component params]
  ;;             (assoc component
  ;;                    :x (:x params)
  ;;                    :y (:y params))))
