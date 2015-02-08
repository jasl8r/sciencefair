(ns sciencefair.routes.dev-helper
  (:require noir.session)
  (:use compojure.core)
  (:require clojure.string)
  (:require noir.response)
  (:require ring.util.response)
  (:require [sciencefair.views.layout :as layout]
            [sciencefair.util :as util]
            [sciencefair.models.db :as db]
            [sciencefair.stripe]))


(defn make-fake []
  {:email1 (str "m" (System/currentTimeMillis) "@example.com") :name1 "Mooky Starks" :phone1 "978-555-1212"
   :email2 (str "t" (System/currentTimeMillis) "@example.com") :name2 "Timmy Buck" :phone2 "212-555-6666" :students 2})


(defn mock-student-data [h4s current limit vec]
  (if (= current limit)
    vec
    (recur h4s (inc current) limit
           (conj vec {:id          current
                      :h4          (get h4s current)
                      :student     (str "student no " current)
                      :school      (str "florence")
                      :grade       (str (inc current))
                      :teacher     (str "teach no " current)
                      :partner     ""
                      :title       (str "experiement # " current)
                      :description (str "can frogs hop " current "?")}))))
