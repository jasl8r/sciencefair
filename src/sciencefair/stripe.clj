(ns sciencefair.stripe
  (:use [taoensso.timbre :only [trace debug info warn error fatal]])
  (:require [clj-http.client :as client]
            [sciencefair.util :as util]
            [environ.core :refer [env]]))


;(println (client/get "http://google.com" ) )

; test payment processing


(defn
  stripe-public-key [] (env :stripe-public-key))

(defn
  stripe-private-key [] (env :stripe-private-key))

(defn process-charge [stripe-token amount]
  (info ["process-charge" stripe-token amount])

  (let [stripe-says (client/post "https://api.stripe.com/v1/charges"
                                 {:basic-auth     [(stripe-private-key) ""]
                                  :body           (str "amount=" amount "00&card=" stripe-token "&currency=usd")
                                  :headers        {"Content-Type" "application/x-www-form-urlencoded"}
                                  :socket-timeout 30000     ;; in milliseconds
                                  :conn-timeout   30000     ;; in milliseconds
                                  :accept         :json})]
    ; Lets save a copy first.
    (info  stripe-says)))