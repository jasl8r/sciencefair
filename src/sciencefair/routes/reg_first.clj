(ns sciencefair.routes.reg-first
  (:require noir.session)
  (:use compojure.core)
  (:use [taoensso.timbre :only [trace debug info warn error fatal]])
  (:require clojure.string)
  (:require noir.response)
  (:require ring.util.response)
  (:require [sciencefair.views.layout :as layout]
            [sciencefair.util :as util]
            [sciencefair.routes.dev-helper :as dev]
            [sciencefair.models.db :as db]
            [sciencefair.stripe]))


(def h4s ["First" "Second" "Third" "Forth"])

(defn make-students-vec [current students data vec validate]
  (defn ex [field]
    (get data (keyword (str field current))))
  (defn is-blank [field]
    (if (and validate (clojure.string/blank? (get data (keyword (str field current)))))
      (str "A " field " is required.")))

  (if (= current students)
    vec
    (recur (inc current) students data (conj vec {:id            current
                                                  :h4            (get h4s current)
                                                  :student       (ex "student")
                                                  :student_error (is-blank "student")
                                                  :school        (ex "school")
                                                  :school_error  (is-blank "school")
                                                  :grade         (ex "grade")
                                                  :grade_error   (is-blank "grade")
                                                  :teacher       (ex "teacher")
                                                  :teacher_error (is-blank "teacher")
                                                  :partner       (ex "partner")
                                                  :partner_error nil
                                                  :title         (ex "title")
                                                  :title_error   (is-blank "title")
                                                  :description   (ex "description")}) validate)))

(defn reg-post [name1 email1 phone1 name2 email2 phone2 students]
  (defn error-render [key message]
    (layout/render "registration.html" {key       message
                                        :name1    name1
                                        :email1   email1
                                        :phone1   phone1
                                        :name2    name2
                                        :email2   email2
                                        :phone2   phone2
                                        :students students}))

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
    (empty? phone1)
    (error-render :error-phone1 "Please provide a contact phone number.")
    (= "0" students)
    (do
      (db/register [email1 name1 phone1 email2 name2 phone2] 0 {} nil nil)
      (layout/render "thanks.html"))
    :else (let [students-as-integer (Integer/parseInt students)]
            (noir.session/assoc-in! [:register-students] [students email1 name1 phone1 email2 name2 phone2])
            (def students-data (if (util/dev-mode?) (dev/mock-student-data h4s 0 students-as-integer []) (make-students-vec 0 students-as-integer {} [] false)))
            (layout/render "registration2.html" {:students students-data}))))


(defn form-data-has-errors [form-data]
  (if-not (seq form-data)
    false
    (let [scrub-nil-values (reduce (fn [m [k v]] (if (nil? v) m (assoc m k v))) {} (first form-data))]
      (if (some #(.contains (str %1) "_error") (keys scrub-nil-values))
        true
        (recur (rest form-data))))))

(defn fetch-reginfo []
  (noir.session/get-in [:registration-info]))

(defn session-registration-add [& args]
  (let [has (fetch-reginfo)
        has-or-empty (if (nil? has) {} has)]
    (noir.session/assoc-in! [:registration-info] (apply assoc has-or-empty args))))

(defn students-post [students-map]
  (if (nil? (noir.session/get-in [:register-students]))
    (layout/render "makechanges.html" {:changes-message true })
    (let [students (first (noir.session/get-in [:register-students]))
          adults (subvec (noir.session/get-in [:register-students]) 1)
          student-as-integer (Integer/parseInt students)
          students-form-data (make-students-vec 0 student-as-integer students-map [] true)]
      (if (form-data-has-errors students-form-data)
        (layout/render "registration2.html" {:students students-form-data})
        (do
          (session-registration-add :adults adults :student-count student-as-integer :students-map students-map)
          (layout/render "payment.html"
                         {:email      (first adults) :student-count student-as-integer
                          :cost       (* 6 student-as-integer)
                          :stripe-key (sciencefair.stripe/stripe-public-key)}))))))

(defn process-payment [params]
  (if (nil? (fetch-reginfo))
    (layout/render "makechanges.html"  {:changes-message true })
    (do
      (session-registration-add :payment-type "cc" :stripe-token (:stripeToken params))
      (layout/render "photopermission.html"))))

(defn record-payment-choice [how]
  (if (nil? (fetch-reginfo))
    (layout/render "makechanges.html"  {:changes-message true })
    (do
      (session-registration-add :payment-type how)
      (layout/render "photopermission.html"))))

(defn record-photopermission [photopermission]
  (if (nil? (fetch-reginfo))
    (layout/render "makechanges.html" {:changes-message true })
    (let [reg-map (noir.session/get-in [:registration-info])
          adults (:adults reg-map)
          primary-email (first adults)
          student-count (:student-count reg-map)
          students-map (:students-map reg-map)]
      ; this prevents double submits, and makes using the back button to edit ineffective.
      (noir.session/clear!)
      (info "registration map" reg-map)
      (db/register adults student-count students-map photopermission (:payment-type reg-map))
      (if (= "cc" (:payment-type reg-map))
        (try
          (sciencefair.stripe/process-charge (:stripe-token reg-map) (* 6 student-count))
          (db/save-paid-by-email primary-email (* 6 student-count))
          (catch RuntimeException ee (prn "Charge issue !!! " ee))))
      (layout/render "thanks.html"))))

