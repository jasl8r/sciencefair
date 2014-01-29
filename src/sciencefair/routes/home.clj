(ns sciencefair.routes.home
  (:require noir.session)
  (:use compojure.core)
  (:use clojure.string)
  (:require clojure.string)
  (:require noir.response)
  (:require [sciencefair.views.layout :as layout]
            [sciencefair.util :as util]
            [sciencefair.models.db :as db]
            ))

(def h4s ["First" "Second" "Third" "Forth"])

(defn make-students-vec [current students data vec validate]
  (defn ex [field]
    (get data (keyword (str field current)))
    )
  (defn is-blank [field]
    (if (and validate (clojure.string/blank? (get data (keyword (str field current)))))
      (str "A " field " is required.")))

  (if (= current students)
    vec
    (recur (inc current) students data (conj vec {:id current
                                                  :h4 (get h4s current)
                                                  :student (ex "student")
                                                  :student_error (is-blank "student")
                                                  :school (ex "school")
                                                  :school_error (is-blank "school")
                                                  :grade (ex "grade")
                                                  :grade_error (is-blank "grade")
                                                  :teacher (ex "teacher")
                                                  :teacher_error (is-blank "teacher")
                                                  :title (ex "title")
                                                  :title_error (is-blank "title")
                                                  :description (ex "description")
                                                  }) validate))

  )

(defn mock-student-data [current limit vec]
  (if (= current limit)
    vec
    (recur (inc current) limit
      (conj vec {:id current
                 :h4 (get h4s current)
                 :student (str "student no " current)
                 :school (str "florence")
                 :grade (str (inc current))
                 :teacher (str "teach no " current)
                 :title (str "experiement # " current)
                 :description (str "can frogs hop " current "?")
                 })))
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
    :else (let [students-as-integer (Integer/parseInt students)]
            (noir.session/assoc-in! [:register-students ] [students email1 name1 email2 name2])
            (def students-data (if (util/dev-mode?) (mock-student-data 0 students-as-integer []) (make-students-vec 0 students-as-integer {} [] false)))
            (layout/render "registration2.html" {:students students-data})
            )
    )
  )


(defn form-data-has-errors [form-data]
  (if-not (seq form-data)
    false
    (let [scrub-nil-values (reduce (fn [m [k v]] (if (nil? v) m (assoc m k v))) {} (first form-data))]
      (if (some #(.contains (str %1) "_error") (keys scrub-nil-values))
        true
        (recur (rest form-data)))
      )))

(defn students-post [args]
  (let [students (first (noir.session/get-in [:register-students ]))
        adults (subvec (noir.session/get-in [:register-students ]) 1)
        student-as-integer (Integer/parseInt students)
        students-form-data (make-students-vec 0 student-as-integer args [] true)]
    (if (form-data-has-errors students-form-data)
      (layout/render "registration2.html" {:students students-form-data})
      (do
        (apply db/register (conj [adults] student-as-integer args))
        (layout/render "thanks.html")
        ))
    )
  )

(defn admin []
  (if (noir.session/get-in [:admin ])
    (layout/render "admin.html" {:students (db/get-students)})
    (layout/render "login.html")
    )
  )

(defn admin-login [password]
  (if (and (not (clojure.string/blank? password))
        (= password (.trim (slurp "/fair-data/adminpass.txt"))))
    (noir.session/assoc-in! [:admin ] true))
  (noir.response/redirect "/a")
  )


(defroutes home-routes
  (GET "/" [] (layout/render "home.html"))
  (GET "/makechanges" [] (layout/render "makechanges.html"))
  (GET "/registration" [] (layout/render "registration.html" (if (util/dev-mode?) {:email1 "mooky@example.com" :name1 "Mooky Starks" :email2 "timbuck@example.com" :name2 "Timmy Buck" :students 2} {})))
  (POST "/regpost" [name1 email1 name2 email2 students] (reg-post name1 email1 name2 email2 students))
  (POST "/students" [& args] (students-post args))
  (GET "/registration2" [] (layout/render "registration2.html"))
  (GET "/thanks" [] (layout/render "thanks.html"))
  (POST "/thanks" [] (layout/render "thanks.html"))
  (GET "/rules" [] (layout/render "rules.html"))
  (GET "/contact" [] (layout/render "contact.html"))
  (GET "/about" [] (layout/render "about.html"))
  (GET "/a" [] (admin))
  (POST "/a" [password] (admin-login password))
  )

