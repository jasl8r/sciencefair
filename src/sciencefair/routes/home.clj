(ns sciencefair.routes.home
  (:require noir.session)
  (:use compojure.core)
  (:use clojure.string)
  (:require [sciencefair.views.layout :as layout]
            [sciencefair.util :as util]
            [sciencefair.models.db :as db]
            ))

;; This is a cop out... trying to meet a deadline.... oh the shame
(defn make-students-vec [students data]
  (cond
    (= students "1")
    [{
       :id "0"
       :title "First"
       :name (get (get data 0) 0)
       :grade (get (get data 0) 1)
       :project (get (get data 0) 2)
       }]
    (= students "2")
    [{
       :id "0"
       :title "First"
       :name (get (get data 0) 0)
       :grade (get (get data 0) 1)
       :project (get (get data 0) 2)
       }
     {
       :id "1"
       :title "Second"
       :name (get (get data 1) 0)
       :grade (get (get data 1) 1)
       :project (get (get data 1) 2)
       }
     ]
    (= students "3")
    [{
       :id "0"
       :title "First"
       :name (get (get data 0) 0)
       :grade (get (get data 0) 1)
       :project (get (get data 0) 2)
       }
     {
       :id "1"
       :title "Second"
       :name (get (get data 1) 0)
       :grade (get (get data 1) 1)
       :project (get (get data 1) 2)
       }
     {
       :id "2"
       :title "Third"
       :name (get (get data 2) 0)
       :grade (get (get data 2) 1)
       :project (get (get data 2) 2)
       }
     ]
    :else
    [{
       :id "0"
       :title "First"
       :name (get (get data 0) 0)
       :grade (get (get data 0) 1)
       :project (get (get data 0) 2)
       }
     {
       :id "1"
       :title "Second"
       :name (get (get data 1) 0)
       :grade (get (get data 1) 1)
       :project (get (get data 1) 2)
       }
     {
       :id "2"
       :title "Third"
       :name (get (get data 2) 0)
       :grade (get (get data 2) 1)
       :project (get (get data 2) 2)
       }
     {
       :id "3"
       :title "Fourth"
       :name (get (get data 3) 0)
       :grade (get (get data 3) 1)
       :project (get (get data 3) 2)
       }
     ]
    )
  )


(defn reg-post [name1 email1 name2 email2 students]

  (defn error-render [key message]
    (layout/render "registration.html" {key message
                                        :name1 name1
                                        :email1 email1
                                        :name2 name2
                                        :email2 email2
                                        :students students})
    )

  (cond
    (not (.isValid (org.apache.commons.validator.routines.EmailValidator/getInstance) email1))
    (error-render :error-email1 "Please enter a valid email.  We use email to send announcements and for registration identity.")
    (db/registered? email1)
    (error-render :error-email1 "This email is already registered.  Use 'Make Changes' on the Home page to update an existing registration")
    (and (not (clojure.string/blank? email2)) (not (.isValid (org.apache.commons.validator.routines.EmailValidator/getInstance) email2)))
    (error-render :error-email2 "Please enter a valid second email.")
    (= email1 email2)
    (error-render :error-email2 "If providing a second email, please make it different than the first email.")
    (db/registered? email2)
    (error-render :error-email2 "This email is already registered.  Use 'Make Changes' to update an existing registration.")
    (empty? students)
    (error-render :error-students "You must select a number of students.")
    (= "0" students)
    (do
      (db/register email1 name1 email2 name2 [])
      (layout/render "thanks.html")
      )
    :else (do
            (noir.session/assoc-in! [:register-students ] [students email1 name1 email2 name2])
            (layout/render "registration2.html" {:students (make-students-vec students [])})
            )
    )
  )

(defn students-post [
                     & args
                     ]
  (prn "student-post-args"
    student0 school0 teach0 grade0 title0 description0
    student1 school1 teach2 grade1 title1 description1
    student2 school2 teach2 grade2 title2 description2
    student3 school3 teach3 grade3 title3 description3)

  (let [students (first (noir.session/get-in [:register-students ]))
        students-int (Integer/parseInt students)
        adults (subvec (noir.session/get-in [:register-students ]) 1)
        student-list (subvec [
                       [student0
                        grade0
                        project0]
                       [student1
                        grade1
                        project1]
                       [student2
                        grade2
                        project2]] 0 students-int) ]

    (defn render-error [message]
      (layout/render "registration2.html" {:error message :students (make-students-vec students student-list)})
      )

    (cond
      (blank? student0) (render-error "Need first student's name")
      (blank? grade0) (render-error "Need first student's grade")
      (blank? project0) (render-error "Need first student's project")
      (and (or (= students "2") (= students "3")) (blank? student1)) (render-error "Need second student's name")
      (and (or (= students "2") (= students "3")) (blank? grade1)) (render-error "Need second student's grade")
      (and (or (= students "2") (= students "3")) (blank? project1)) (render-error "Need second student's project")
      (and (= students "3") (blank? student2)) (render-error "Need third student's name")
      (and (= students "3") (blank? grade2)) (render-error "Need third student's grade")
      (and (= students "3") (blank? project2)) (render-error "Need third student's project")
      :else (do
              (apply db/register (conj adults student-list))
              (layout/render "thanks.html")
              )
      )
    )
  )

(defn admin []
  (layout/render "admin.html" {:students (db/get-students)})
 )


(defroutes home-routes
  (GET "/" [] (layout/render "home.html"))
  (GET "/makechanges" [] (layout/render "makechanges.html"))
  (GET "/registration" [] (layout/render "registration.html" (if (util/dev-mode?) {:email1 "mooky@example.com" :name1 "Mooky Starks" :students "1" } {})))
  (POST "/regpost" [name1 email1 name2 email2 students] (reg-post name1 email1 name2 email2 students))
  (POST "/students" [student0 grade0 project0 student1 grade1 project1 student2 grade2 project2] (students-post student0 grade0 project0 student1 grade1 project1 student2 grade2 project2))
  (GET "/registration2" [] (layout/render "registration2.html"))
  (GET "/thanks" [] (layout/render "thanks.html"))
  (POST "/thanks" [] (layout/render "thanks.html"))
  (GET "/rules" [] (layout/render "rules.html"))
  (GET "/contact" [] (layout/render "contact.html"))
  (GET "/unsubscribe" [] (layout/render "unsubscribe.html"))
  (GET "/about" [] (layout/render "about.html"))
  (GET "/a" [] (admin))
  )

