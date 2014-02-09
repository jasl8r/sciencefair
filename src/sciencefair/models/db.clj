(ns sciencefair.models.db
  (:use clojure.string)
  (:require [clojure.java.jdbc :as sql]
            [noir.session]
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
  (sql/query db-spec ["select b.student, b.school, b.grade, b.teacher, b.title, b.description, a.name, a.email, d.name as 'secondary' , d.email AS 'secondary', a.created_date, a.paid
                      from adults a join students b on a.id = b.adult_id left join adults d on a.id = d.first_id order by a.created_date"])
  )

(defn get-registration-as-form [email]
  (let [adult (first (sql/query db-spec ["select * from adults where email=?" email]))
        adult2 (if (nil? (:first_id adult))
                 (first (sql/query db-spec ["select * from adults where first_id = ?" (:id adult)]))
                 (first (sql/query db-spec ["select * from adults where id = ?" (:first_id adult)])))
        [primary secondary] (if (nil? (:first_id adult)) [adult adult2] [adult2 adult])
        students (sql/query db-spec ["select * from students where adult_id = ?" (:id primary)])
        ]
    {:paid (if (nil? (:paid primary)) "$0" (str "$" (:paid primary))) :email1 (:email primary) :name1 (:name primary) :email2 (:email secondary) :name2 (:name secondary)
     :students students
     }
    )
  )

(defn get-student [id]
  (first (sql/query db-spec ["select * from students where id = ?" id]))
  )

(defn update-student [smap]

  ; {:description "", :title "tinkering with heat", :teacher "Gurnsey", :grade "3",
  ;      :school "florence", :student "Liam Herrmann", :id "2"}

  (sql/execute! db-spec ["update students set student=?, school=?, grade=?,teacher=?, title=?, description=?, updated_date=now() where id = ?"
                         (:student smap) (:school smap) (:grade smap)
                         (:teacher smap) (:title smap) (:description smap)
                         (:id smap)])

  )

(defn get-primary-adult [email]
  (let [adult (first (sql/query db-spec ["select * from adults where email=?" email]))]
    (if (nil? (:first_id adult))
      adult
      (first (sql/query db-spec ["select * from adults where id = ?" (:first_id adult)])))))




(defn has-student-access [id]
  (let [email (noir.session/get-in [:edit-reg ])]
    (if (nil? email)
      false
      (let [primary-adult (get-primary-adult email)]
        (= (:adult_id (get-student id)) (:id primary-adult))))))

