(ns sciencefair.routes.admin
  (:require noir.session)
  (:use compojure.core)
  (:require clojure.string)
  (:require noir.response)
  (:require ring.util.response)
  (:require [sciencefair.views.layout :as layout]
            [sciencefair.util :as util]
            [sciencefair.models.db :as db]
            [sciencefair.stripe]
            [environ.core :refer [env]]))

(defn admin-login [password]
  (if (and (not (clojure.string/blank? password))
           (= password (env :admin-pass)))
    (noir.session/assoc-in! [:admin] true))
  (noir.response/redirect "/a"))

(defn admin []
  (if (noir.session/get-in [:admin])
    (layout/render "/admin/students.html" {:students (db/get-students)})
    (layout/render "/admin/login.html")))

(defn adults-get []
  (if-not (noir.session/get-in [:admin])
    (layout/render "/admin/login.html")
    (let [adults (db/get-adults)
          adults-with-link (map #(assoc % :email-link (util/make-email-link (:email %) )) adults )
          ]
      (layout/render "/admin/adults.html" {:adults adults-with-link}))))

(defn adults-post [arg]
  (if-not (noir.session/get-in [:admin])
    (noir.response/redirect "/a")
    (do
      (db/save-paid (first arg))
      (layout/render "/admin/adults.html" {:adults (db/get-adults)}))))

(def lists [["all emails" "from adults a"]
            ["primary email of unpaid" "from adults a, students s where a.id = s.adult_id and a.paid is null or trim(a.paid) = ''"]])

(defn make-lists-summary []
  (map #(hash-map :name (get %1 0) :size (db/list-count (get %1 1))) lists))

(defn email-lists [id]
  (if-not (noir.session/get-in [:admin])
    (noir.response/redirect "/a")
    (if-not (nil? id)
      (ring.util.response/response (clojure.string/join "," (db/list-fetch (second (first (filter #(= id (first %)) lists))))))
      (do
        (layout/render "/admin/lists.html" {:lists (make-lists-summary)})))))

(defn all-students-csv []
  (if-not (noir.session/get-in [:admin])
    (noir.response/redirect "/a")
    (noir.response/set-headers {"Content-Disposition"
                                "attachment; filename=students.csv"}
                               (noir.response/content-type
                                "application/csv"
                                (db/all-students-csv)))))

; not sure where this belongs... admin and editreg both need it
(defn logout-now []
  (noir.session/clear!)
  (noir.response/redirect "/"))
