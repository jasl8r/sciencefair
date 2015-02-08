(ns sciencefair.stripe
  (:use [taoensso.timbre :only [trace debug info warn error fatal]])
  (:require [clj-http.client :as client]
            [sciencefair.util :as util]))


;(println (client/get "http://google.com" ) )

; test payment processing


(defn
  stripe-public-key []
  (.trim (slurp
          (if (util/dev-mode?)
            "/fair-data/stripe-public-test.key"
            "/fair-data/stripe-public-live.key"))))

(defn
  stripe-private-key []
  (.trim (slurp
          (if (util/dev-mode?)
            "/fair-data/stripe-private-test.key"
            "/fair-data/stripe-private-live.key"))))

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