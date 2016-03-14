(ns sciencefair.models.db
  (:require clojure.string)
  (:require [clojure.java.jdbc :as sql]
            [noir.session]
            [sciencefair.util]))

(def db-spec
  {:subprotocol "mysql"
   :subname     "//localhost/sciencefair"
   :user        (.trim (slurp "/fair-data/dbuser.txt"))
   :password    (.trim (slurp "/fair-data/dbpass.txt"))})


(defn registered? [email]
  (< 0 (:count (first (sql/query db-spec ["select count(*) as count from adults where email = ?" email])))))

(defn lookup-id [email]
  (:id (first (sql/query db-spec ["select id from adults where email = ?" email]))))


(defn record-payment-choice [email how]
  (sql/execute! db-spec ["update adults set payment_choice=? where email = ?"
                         how email]))

(defn record-photopermission [email photopermission]
  (sql/execute! db-spec ["update adults set photo_permission=? where email = ?"
                         photopermission email]))

(defn register [adults student-count students-map photo-permission payment-type]
  (let [[email1 name1 phone1 email2 name2 phone2] adults]
    (sql/execute! db-spec ["insert into adults (email, name, created_date, phone, photo_permission, payment_choice) values (?, ?, now(), ?, ?, ?)" email1 name1 phone1 photo-permission  payment-type])
    (def first-adult-id (lookup-id email1))
    (if (not (clojure.string/blank? email2))
      (sql/execute! db-spec ["insert into adults (email, name, first_id, created_date, phone) values (?, ?, ?, now(), ?)" email2 name2 first-adult-id phone2]))

    (def colnames [:student, :school, :grade, :teacher, :partner, :title, :description])
    (defn col-names []
      (clojure.string/join ", " (map name colnames)))
    (defn extract-value [dex col-name]
      ((keyword (str (name col-name) dex)) students-map))
    (defn col-values [dex]
      (map #(extract-value dex %1) colnames))
    (dotimes [dex student-count]
      (sql/execute! db-spec (concat [(str "insert into students ( adult_id, " (col-names) ", created_date ) values ( ?, ?, ?, ?, ?, ?, ?, ?, now() )") first-adult-id] (col-values dex))))

    (sciencefair.util/send-email-confirmation email1 name1)
    (if (not (clojure.string/blank? email2))
      (sciencefair.util/send-email-confirmation email2 name2))))

(defn get-registration-as-form [email]
  (let [adult (first (sql/query db-spec ["select * from adults where email=?" email]))
        adult2 (if (nil? (:first_id adult))
                 (first (sql/query db-spec ["select * from adults where first_id = ?" (:id adult)]))
                 (first (sql/query db-spec ["select * from adults where id = ?" (:first_id adult)])))
        [primary secondary] (if (nil? (:first_id adult)) [adult adult2] [adult2 adult])
        students (sql/query db-spec ["select * from students where adult_id = ?" (:id primary)])]
    {:paid     (if (nil? (:paid primary)) "$0" (str "$" (:paid primary)))
     :photo_permission (:photo_permission primary)
     :email1   (:email primary) :name1 (:name primary) :phone1 (:phone primary) :email2 (:email secondary) :name2 (:name secondary) :phone2 (:phone secondary)
     :students students}))

(defn get-student [id]
  (first (sql/query db-spec ["select * from students where id = ?" id])))

(defn update-student [smap]

  ; {:description "", :title "tinkering with heat", :teacher "Gurnsey", :grade "3",
  ;      :school "florence", :student "Liam Herrmann", :id "2"}
  
  (sql/execute! db-spec ["update students set student=?, school=?, grade=?,teacher=?, partner=?, title=?, description=?, updated_date=now() where id = ?"
                         (:student smap) (:school smap) (:grade smap)
                         (:teacher smap) (:partner smap) (:title smap) (:description smap)
                         (:id smap)]))

(defn get-primary-adult [email]
  (let [adult (first (sql/query db-spec ["select * from adults where email=?" email]))]
    (if (nil? (:first_id adult))
      adult
      (first (sql/query db-spec ["select * from adults where id = ?" (:first_id adult)])))))

(defn has-student-access [id]
  (let [email (noir.session/get-in [:edit-reg])]
    (if (nil? email)
      false
      (let [primary-adult (get-primary-adult email)]
        (= (:adult_id (get-student id)) (:id primary-adult))))))

(defn remove-student [id]
  (sql/execute! db-spec ["delete from students where id = ?" id]))

(defn get-primary-adult-session []
  (let [email (noir.session/get-in [:edit-reg])]
    (get-primary-adult email)))

(defn add-student [adult-id data]
  (sql/execute! db-spec [(str "insert into students ( adult_id, description, title, teacher, partner, grade, school, student, created_date )"
                              " values (?,?,?,?,?,?,?,?,now())") adult-id (:description data) (:title data)  (:teacher data) (:partner data)
                         (:grade data) (:school data) (:student data)]))

(defn get-adults []
  (sql/query db-spec [(str "select a.id, a.name, a.email, a.phone, d.name, d.email, d.phone, a.paid, a.payment_choice, a.photo_permission, (select count(*) from students where adult_id = a.id) as students "
                           " from adults a left join adults d on a.id = d.first_id where a.first_id is null order by a.created_date")]))

(defn save-paid [args]
  (doseq [[key value] args]
    (if (.startsWith (name key) "paid")
      (let [id (.substring (name key) 4)
            val (if (empty? value) nil value)]
        (sql/execute! db-spec ["update adults set paid=? where id = ?" val id])))))

(defn save-paid-by-email [email amount]
  (sql/execute! db-spec ["update adults set paid=? where email = ?" amount email]))


(defn list-count [where-clause]
  (:c (first (sql/query db-spec [(str "select count(a.email) as c " where-clause)]))))

(defn list-fetch [where-clause]
  (map #(:email %) (sql/query db-spec [(str "select a.email " where-clause)])))


(defn get-students []
  (sql/query db-spec ["select b.student, b.school, b.grade, b.teacher, b.partner, b.title, b.description, a.name, d.name as 'secondary', a.created_date, a.paid
                      from adults a join students b on a.id = b.adult_id left join adults d on a.id = d.first_id order by a.created_date"]))

(defn make-student-row [row]
  [(:student row)
   (:school row)
   (:grade row)
   (:title row)
   (:partner row)
   (:description row)
   (:name row)
   (:secondary row)
   (.toString (:created_date row))
   (:paid row)])

(defn make-comma-sep-quoted [row]
  (clojure.string/join "," (map #(str "\"" (.replaceAll (str (if (nil? %1) "" %1)) "\"" "\"\"") "\"") row)))

(defn all-students-csv []
  (let [rows (concat [["Student" "School" "Grade" "Project" "Partner" "Description" "Parent 1" "Parent 2" "Created" "Paid"]] (into [] (map make-student-row (get-students))))
        rows-comma-sep (map #(make-comma-sep-quoted %) rows)
        csv-file (clojure.string/join "\n" rows-comma-sep)]
    csv-file))