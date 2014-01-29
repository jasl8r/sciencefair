(ns sciencefair.models.db
  (:use clojure.string)
  (:require [clojure.java.jdbc :as sql]
            [sciencefair.util]))

(def db-spec
  {:subprotocol "mysql"
   :subname "//localhost/sciencefair"
   :user "root"
   :password (.trim (slurp "/fair-data/dbpass.txt"))})


(defn registered? [email]
  (< 0 (:count (first (sql/query db-spec ["select count(*) as count from adults where email = ?" email]))))
  )

(defn lookup-id [email]
  (:id (first (sql/query db-spec ["select id from adults where email = ?" email])))
  )

(defn register [adults student-count students-map]
  (prn "Asked to register: " adults student-count students-map)

  (let [[email1 name1 email2 name2] adults]
    (sql/execute! db-spec ["insert into adults (email, name, created_date) values (?, ?, now())" email1 name1])
    (def first-adult-id (lookup-id email1))
    (if (not (blank? email2))
      (sql/execute! db-spec ["insert into adults (email, name, first_id, created_date) values (?, ?, ?, now())" email2 name2 first-adult-id])
      )

    (def colnames [:student, :school, :grade, :teacher, :title, :description ])
    (defn col-names []
      (clojure.string/join ", " (map name colnames))
      )
    (defn extract-value [dex col-name]
      ((keyword (str (name col-name) dex)) students-map)
      )
    (defn col-values [dex]
      (map #(extract-value dex %1) colnames)
      )
    (dotimes [dex student-count]
      (sql/execute! db-spec (concat [(str "insert into students ( adult_id, " (col-names) ", created_date ) values ( ?, ?, ?, ?, ?, ?, ?, now() )") first-adult-id] (col-values dex))))

    (sciencefair.util/send-email-confirmation email1 name1)
    (if (not (blank? email2))
      (sciencefair.util/send-email-confirmation email2 name2))
    )
  )

(defn get-students []
  (sql/query db-spec ["select b.student, b.school, b.grade, b.teacher, b.title, b.description, a.name, a.email as 'secondary' , d.name AS 'secondary', d.email, a.created_date, a.paid
                      from adults a join students b on a.id = b.adult_id left join adults d on a.id = d.first_id order by a.name"] )
  )