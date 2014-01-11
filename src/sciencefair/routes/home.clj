(ns sciencefair.routes.home
  (:use noir.session)
  (:use compojure.core)
  (:require [sciencefair.views.layout :as layout]
            [sciencefair.util :as util]))

(defn reg-post [name1 email1 name2 email2 students]
  (prn "name1" name1)
  (prn "email1" email1)
  (prn "name2" name2)
  (prn "email2" email2)
  (prn "students" students)
  (assoc-in! :register-first [name1 email1 name2 email2 students])

  (layout/render "registration2.html")
  )

(defroutes home-routes
  (GET "/" [] (layout/render "home.html" ))
  (GET "/login" [] (layout/render "login.html"))
  (GET "/registration" [] (layout/render "registration.html"))
  (POST "/regpost" [name1 email1 name2 email2 students] (reg-post name1 email1 name2 email2 students))
  (GET "/registration2" [] (layout/render "registration2.html"))
  (GET "/thanks" []  (layout/render "thanks.html"))
  (GET "/faq" [] (layout/render "faq.html"))
  (GET "/rules" [] (layout/render "rules.html"))
  (GET "/contact" [] (layout/render "contact.html"))
  (GET "/about" [] (layout/render "about.html"))
  )

