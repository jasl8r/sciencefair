(ns sciencefair.routes.home
  (:require noir.session)
  (:use compojure.core)
  (:require clojure.string)
  (:require noir.response)
  (:require ring.util.response)
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
    (error-render :error-email1 "This email is already registered.  Use 'My Registration' link at the top to view/edit an existing registration.")
    (and (not (clojure.string/blank? email2)) (not (.isValid (org.apache.commons.validator.routines.EmailValidator/getInstance) email2)))
    (error-render :error-email2 "Please enter a valid second email.")
    (= email1 email2)
    (error-render :error-email2 "If providing a second email, please make it different than the first email.")
    (db/registered? email2)
    (error-render :error-email2 "This email is already registered.  Use 'My Registration' link at the top to view/edit an existing registration.")
    (empty? students)
    (error-render :error-students "You must select a number of students.")
    (= "0" students)
    (do
      (db/register [email1 name1 email2 name2] 0 {})
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
    (layout/render "/admin/students.html" {:students (db/get-students)})
    (layout/render "/admin/login.html")
    )
  )

(defn admin-login [password]
  (if (and (not (clojure.string/blank? password))
        (= password (.trim (slurp "/fair-data/adminpass.txt"))))
    (noir.session/assoc-in! [:admin ] true))
  (noir.response/redirect "/a")
  )

(defn make-changes-request [email]
  (if (not (.isValid (org.apache.commons.validator.routines.EmailValidator/getInstance) email))
    (layout/render "makechanges2.html" {:error "That is not a vaild email address" :email email})
    (if-not (db/registered? email)
      (layout/render "makechanges2.html" {:error "Sorry, that email address is not on registered with us." :email email})
      (do
        (util/send-make-changes-link email)
        (layout/render "checkyouremail.html")
        )
      )
    )
  )

(defn editreg [e h]
  (if (empty? e)
    (let [ee (noir.session/get-in [:edit-reg ])]
      (if (empty? ee)
        (layout/render "problem.html")
        (layout/render "editreg.html" (conj (db/get-registration-as-form ee) {:message (noir.session/flash-get :message )}))))
    (if-not (= (util/make-md5-hash e) h)
      (layout/render "problem.html")
      (do
        (noir.session/assoc-in! [:edit-reg ] e)
        (layout/render "editreg.html" (db/get-registration-as-form e))
        ))))

(defn editreg-post [args]
  (let [e (noir.session/get-in [:edit-reg ])]
    (layout/render "editreg.html" (db/get-registration-as-form e)))
  )

(defn edit-student [id]
  (if-not (db/has-student-access id)
    (layout/render "security-problem.html")
    (layout/render "edit-student.html" {:item (db/get-student id)})
    ))

(defn edit-student-post [args]
  (if-not (db/has-student-access (:id args))
    (layout/render "security-problem.html")
    (do
      ; validate fields
      ;(layout/render "edit-student.html" { :item args } )
      (db/update-student args)
      (noir.session/flash-put! :message (str "Student \"" (:student args) "\" updated!"))
      (noir.response/redirect "/editreg")
      )
    )
  )

(defn make-changes-click []
  (let [email (noir.session/get-in [:edit-reg ])]
    (if (nil? email)
      (layout/render "makechanges2.html")
      (layout/render "editreg.html" (db/get-registration-as-form email))))
  )

(defn logout-now []
  (noir.session/clear!)
  (noir.response/redirect "/")
  )

(defn remove-student [id]
  (if-not (db/has-student-access id)
    (layout/render "security-problem.html")
    (do
      (db/remove-student id)
      (noir.session/flash-put! :message "Student removed!")
      (noir.response/redirect "/editreg")
      )
    ))


(defn validate-student [required-fields student-map]
  (if (empty? required-fields)
    student-map
    (let [field-name (first required-fields)]
      (if-not (clojure.string/blank? (field-name student-map))
        (recur (rest required-fields) student-map)
        (recur (rest required-fields) (conj student-map {(keyword (str (name field-name) "_error")) (str "A " (name field-name) " is required.") :error true}))
        ))))

(defn add-student-post [args]
  (let [required-fields [:school :title :teacher :student, :grade :title ]
        data-with-errors (validate-student required-fields args)
        primary-adult (db/get-primary-adult-session)]
    (if (nil? primary-adult)
      (layout/render "security-problem.html")
      (if (= true (:error data-with-errors))
        (layout/render "add-student.html" {:item data-with-errors})
        (do
          (db/add-student (:id primary-adult) data-with-errors)
          (noir.session/flash-put! :message "Student added!")
          (noir.response/redirect "/editreg")
          )
        ))))

(defn adults-get []
  (if-not (noir.session/get-in [:admin ])
    (layout/render "/admin/login.html")
    (let [adults (db/get-adults)]
      (layout/render "/admin/adults.html" {:adults adults}))))

(defn adults-post [arg]
  (if-not (noir.session/get-in [:admin ])
    (noir.response/redirect "/a")
    (do
      (db/save-paid (first arg))
      (layout/render "/admin/adults.html" {:adults (db/get-adults)}))))

(def lists [
             ["all emails" "from adults a"]
             ["primary email of unpaid" "from adults a, students s where a.id = s.adult_id and a.paid is null or trim(a.paid) = ''"]
             ])

(defn make-lists-summary []
  (map #(hash-map :name (get %1 0) :size (db/list-count (get %1 1))) lists)
  )

(defn email-lists [id]
  (if-not (noir.session/get-in [:admin ])
    (noir.response/redirect "/a")
    (if-not (nil? id)
      (ring.util.response/response (clojure.string/join "," (db/list-fetch (second (first (filter #(= id (first %)) lists))))))
      (do
        (layout/render "/admin/lists.html" {:lists (make-lists-summary)})
        )
      ))
  )

(defn all-students-csv []
  (if-not (noir.session/get-in [:admin ])
    (noir.response/redirect "/a")
    (noir.response/set-headers {
                                 "Content-Disposition"
                                 "attachment; filename=students.csv"}
      (noir.response/content-type
        "application/csv"
        (db/all-students-csv)))))

(defroutes home-routes
  (GET "/" [] (layout/render "home.html"))
  ;  (GET "/makechanges" [] (if (util/dev-mode?) (layout/render "makechanges2.html") (layout/render "makechanges.html")))
  (GET "/makechanges" [] (make-changes-click))
  (POST "/makechanges" [email] (make-changes-request email))
  (GET "/waitinglist" [] (layout/render "waitinglist.html"))
  (GET "/registration" [] (layout/render "registration.html" (if (util/dev-mode?) {:email1 "mooky@example.com" :name1 "Mooky Starks" :email2 "timbuck@example.com" :name2 "Timmy Buck" :students 2} {})))
  (POST "/regpost" [name1 email1 name2 email2 students] (reg-post name1 email1 name2 email2 students))
  (POST "/students" [& args] (students-post args))
  (GET "/registration2" [] (layout/render "registration2.html"))
  (GET "/thanks" [] (layout/render "thanks.html"))
  (POST "/thanks" [] (layout/render "thanks.html"))
  (GET "/rules" [] (layout/render "rules.html"))
  (GET "/contact" [] (layout/render "contact.html"))
  (GET "/info" [] (layout/render "info.html"))
  (GET "/a" [] (admin))
  (POST "/a" [password] (admin-login password))
  (GET "/editreg" [e h] (editreg e h))
  (POST "/editreg" [& args] (editreg-post args))
  (GET "/edit-student" [id] (edit-student id))
  (POST "/edit-student" [& args] (edit-student-post args))
  (GET "/logout" [] (logout-now))
  (GET "/remove-student" [id] (remove-student id))
  (GET "/add-student" [] (layout/render "add-student.html"))
  (POST "/add-student" [& args] (add-student-post args))
  (GET "/adults" [] (adults-get))
  (POST "/adults" [& args] (adults-post [args]))
  (GET "/lists" [id] (email-lists id))
  (GET "/all-students-csv" [] (all-students-csv) )
  )

