(ns sciencefair.routes.reg-edit
  (:require noir.session)
  (:use compojure.core)
  (:require clojure.string)
  (:require noir.response)
  (:require ring.util.response)
  (:require [sciencefair.views.layout :as layout]
            [sciencefair.util :as util]
            [sciencefair.models.db :as db]
            [sciencefair.stripe]))

  ;  Editing an existing registration

(defn make-changes-click []
  (let [email (noir.session/get-in [:edit-reg])]
    (if (nil? email)
      (layout/render "makechanges.html")
      (layout/render "editreg.html" (db/get-registration-as-form email)))))


(defn make-changes-request [email]
  (if (not (.isValid (org.apache.commons.validator.routines.EmailValidator/getInstance) email))
    (layout/render "makechanges.html" {:error "That is not a vaild email address" :email email})
    (if-not (db/registered? email)
      (layout/render "makechanges.html" {:error "Sorry, that email address is not registered with us." :email email})
      (let [email-link (util/send-make-changes-link email)]
        (layout/render "checkyouremail.html" (if (util/dev-mode?) {:email-link email-link} {}))))))


(defn editreg [e h]
  (if (empty? e)
    (let [ee (noir.session/get-in [:edit-reg])]
      (if (empty? ee)
        (layout/render "problem.html")
        (layout/render "editreg.html" (conj (db/get-registration-as-form ee) {:message (noir.session/flash-get :message)}))))
    (if-not (= (util/make-md5-hash e) h)
      (layout/render "problem.html")
      (do
        (noir.session/assoc-in! [:edit-reg] e)
        (layout/render "editreg.html" (db/get-registration-as-form e))))))


(defn edit-student [id]
  (if-not (db/has-student-access id)
    (layout/render "security-problem.html")
    (layout/render "edit-student.html" {:item (db/get-student id)})))

(defn edit-student-post [args]
  (if-not (db/has-student-access (:id args))
    (layout/render "security-problem.html")
    (do
      ; validate fields
      ;(layout/render "edit-student.html" { :item args } )
      (db/update-student args)
      (noir.session/flash-put! :message (str "Student \"" (:student args) "\" updated!"))
      (noir.response/redirect "/editreg"))))



(defn remove-student [id]
  (if-not (db/has-student-access id)
    (layout/render "security-problem.html")
    (do
      (db/remove-student id)
      (noir.session/flash-put! :message "Student removed!")
      (noir.response/redirect "/editreg"))))


(defn validate-student [required-fields student-map]
  (if (empty? required-fields)
    student-map
    (let [field-name (first required-fields)]
      (if-not (clojure.string/blank? (field-name student-map))
        (recur (rest required-fields) student-map)
        (recur (rest required-fields) (conj student-map {(keyword (str (name field-name) "_error")) (str "A " (name field-name) " is required.") :error true}))))))

(defn add-student-post [args]
  (let [required-fields [:school :title :teacher :student, :grade :title]
        data-with-errors (validate-student required-fields args)
        primary-adult (db/get-primary-adult-session)]
    (if (nil? primary-adult)
      (layout/render "security-problem.html")
      (if (= true (:error data-with-errors))
        (layout/render "add-student.html" {:item data-with-errors})
        (do
          (db/add-student (:id primary-adult) data-with-errors)
          (noir.session/flash-put! :message "Student added!")
          (noir.response/redirect "/editreg"))))))

