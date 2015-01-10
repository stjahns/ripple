(ns dungeon-sandbox.asset-database-test
  (:use [clojure.test])
  (:require [dungeon-sandbox.asset-database :as asset-db]))

;; Core asset stuff

(deftest load-asset-file

  (testing "with test-asset.yaml"
    (let [parsed-assets (#'asset-db/load-asset-file "resources/test/test-asset.yaml")
          asset (first parsed-assets)]
      (testing "should set asset"
        (is (= (:asset asset) "test-asset")))
      (testing "should set name"
        (is (= (:name asset) "TestAsset"))))))

(deftest def-asset

  (asset-db/defasset TestAsset
    :create (fn [system params]
              "CREATED ASSET"))

  (testing "should register the asset definition"
    (is (not= (asset-db/get-asset-def 'TestAsset)
              nil))))

(deftest get-asset

  (asset-db/defasset test-asset
    :create (fn [system params]
              "CREATED ASSET"))

  (let [system (asset-db/start {} "resources/test/test-asset.yaml")]
    (testing "should return created asset"
      (is (= (asset-db/get-asset system "test-asset")
             "CREATED ASSET")))))

;; Tests for texture assets?
