(ns sciencefair.routes.home
  (:require noir.session)
  (:use compojure.core)
  (:require clojure.string)
  (:require noir.response)
  (:require ring.util.response)
  (:use sciencefair.routes.admin)
  (:use sciencefair.routes.reg-edit)
  (:use sciencefair.routes.reg-first)
  (:use sciencefair.routes.dev-helper)
  (:require [sciencefair.views.layout :as layout]
            [sciencefair.util :as util]
            [sciencefair.models.db :as db]
            [sciencefair.stripe]
            [environ.core :refer [env]]))

(defroutes home-routes

  ; Informational
  (GET "/" [] (layout/render "home.html"))
  (GET "/rules" [] (layout/render "rules.html"))
  (GET "/contact" [] (layout/render "contact.html"))
  (GET "/info" [] (layout/render "info.html"))

   ; New Registration  - this are in typical invocation order
  (GET "/waitinglist" [] (layout/render "waitinglist.html")) ; Used when past the signup deadline, currently inactive
  (GET "/registration" []
    (if (= (env :registration-open) "true")
      (layout/render "registration.html" (if (util/dev-mode?) (make-fake) {}))
      (layout/render "registration-soon.html")))
  (GET "/registration2" [] (layout/render "registration2.html"))
  (POST "/regpost" [name1 email1 phone1 name2 email2 phone2 students] (reg-post name1 email1 phone1 name2 email2 phone2 students))

  (POST "/students" [& args] (students-post args))
  (POST "/process-payment" [& args] (process-payment args))
  (POST "/record-payment-choice" [how] (record-payment-choice how))
  (POST "/record-photo-permission" [photopermission] (record-photopermission photopermission))

   ; Editing an existing registration
  (GET "/makechanges" [] (make-changes-click))
  (POST "/makechanges" [email] (make-changes-request email))
  (GET "/editreg" [e h] (editreg e h))
  ;(POST "/editreg" [& args] (editreg-post args))
  (GET "/edit-student" [id] (edit-student id))
  (POST "/edit-student" [& args] (edit-student-post args))
  (GET "/remove-student" [id] (remove-student id))
  (GET "/add-student" [] (layout/render "add-student.html"))
  (POST "/add-student" [& args] (add-student-post args))
  (GET "/logout" [] (logout-now))


   ; Admin
  (GET "/a" [] (admin))
  (POST "/a" [password] (admin-login password))
  (GET "/lists" [id] (email-lists id))
  (GET "/all-students-csv" [] (all-students-csv))
  (GET "/adults" [] (adults-get))
  (POST "/adults" [& args] (adults-post [args])))

