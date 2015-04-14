(ns ripple.vector
  (:require [clojure.math.numeric-tower :as math]))

(defn add [[x1 y1] [x2 y2]]
  [(+ x1 x2) (+ y1 y2)])

(defn sub [[x1 y1] [x2 y2]]
  [(- x1 x2) (- y1 y2)])

(defn norm [[x y]]
  (math/sqrt (+ (* x x)
                (* y y))))

(defn normalized [[x y]]
  (let [n (norm [x y])]
    [(/ x n) (/ y n)]))

(defn scale [[x y] s]
  [(* x s) (* y s)]):w

(defn lerp
  "Linear interpolation between two vectors according to parameter t"
  [[x1 y1] [x2 y2] t]
  [(+ x1 (* t (- x2 x1)))
   (+ y1 (* t (- y2 y1)))])
