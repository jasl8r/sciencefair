(ns sciencefair.models.db
  (:use clojure.string)
  (:require [clojure.java.jdbc :as sql]
            [sciencefair.util]))

; create table adults (id int not null auto_increment, email varchar(100), name varchar(100), first_id int, created_date datetime, updated_date datetime, paid int, PRIMARY KEY(id));
; create table students (id int not null auto_increment, adult_id int, name varchar(100), grade varchar(2), project varchar(200), created_date datetime, updated_date datetime, PRIMARY KEY(id));

(def db-spec
  {:subprotocol "mysql"
   :subname "//localhost/sciencefair"
   :user "root"
   :password (.trim (slurp "dbpass.txt"))})


(defn registered? [email]
  (< 0 (:count (first (sql/query db-spec ["select count(*) as count from adults where email = ?" email]))))
  )

(defn lookup-id [email]
  (:id (first (sql/query db-spec ["select id from adults where email = ?" email])))
  )

(defn register [email1 name1 email2 name2 students]
  (prn "Asked to register: " email1 name1 email2 name2 students)

  (sql/execute! db-spec ["insert into adults (email, name, created_date) values (?, ?, now())" email1 name1])
  (def first-adult-id (lookup-id email1))
  (if (not (blank? email2))
    (sql/execute! db-spec ["insert into adults (email, name, first_id, created_date) values (?, ?, ?, now())" email2 name2 first-adult-id])
    )

  ;; insert students
  (doseq [[name grade project] students]
    (sql/execute! db-spec ["insert into students (adult_id, name, grade, project, created_date) values (?, ?, ?, ?, now())"
                           first-adult-id name grade project]))

  (sciencefair.util/send-email-confirmation email1 name1)
  (if (not (blank? email2))
    (sciencefair.util/send-email-confirmation email2 name2))

  )


(defn get-students []
  (sql/query db-spec ["select b.name, b.grade, b.project, a.name, a.email, a.created_date, a.paid from adults a, students b where a.id = b.adult_id"])
  )