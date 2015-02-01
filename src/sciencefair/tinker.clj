(ns sciencefair.tinker
  (:require [clj-http.client :as client]))

;(println (client/get "http://google.com" ) )

; test payment processing

(println (client/post "https://api.stripe.com/v1/charges"
                      {:basic-auth     ["iIlkF1hgFe9uegsF7w6OinE09qhGzLUn" ""]
                       :body           "amount=400&card=tok_5c5qNFUtN0e1lC&currency=usd"
                       :headers        {"Idempotency-Key" "Zkeqvt6vNoyANubW"
                                        "Content-Type"  "application/x-www-form-urlencoded"}
                       :socket-timeout 30000                 ;; in milliseconds
                       :conn-timeout   30000                 ;; in milliseconds
                       :accept         :json}))


