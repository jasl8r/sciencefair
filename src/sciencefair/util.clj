(ns sciencefair.util
  (:require [noir.io :as io]
            [noir.session]
            [markdown.core :as md]
            [environ.core :refer [env]]))

(import 'org.apache.commons.mail.SimpleEmail)

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
  (->>
   (io/slurp-resource filename)
   (md/md-to-html-string)))

(defn dev-mode? [] (= (env :dev-mode) "true"))

(defn get-smtp-pass [] (env :smtp-pass))

; https://gist.github.com/eliasson/1302024
(defn md5
  "Generate a md5 checksum for the given string"
  [token]
  (let [hash-bytes
        (doto (java.security.MessageDigest/getInstance "MD5")
          (.reset)
          (.update (.getBytes token)))]
    (.toString
     (new java.math.BigInteger 1 (.digest hash-bytes)) ; Positive and the size of the number
     16))) ; Use base16 i.e. hex

(defn send-email [email-addr email-name subject message]
  (doto (SimpleEmail.)
    (.setHostName (env :smtp-host))
    (.setSSL (= (env :smtp-ssl) "true"))
    (.setTLS (= (env :smtp-tls) "true"))
    (.setSslSmtpPort (env :smtp-port))
    (.setSmtpPort (Integer/parseInt (env :smtp-port)))
    (.addTo email-addr email-name)
    (.setFrom (env :smtp-from) (env :smtp-name))
    (.setSubject subject)
    (.setMsg message)
    (.setAuthentication (env :smtp-user) (get-smtp-pass))
    (.send)))

(defn send-email-confirmation [email-addr email-name]
  (send-email email-addr email-name
              "Science Fair Signup Confirmation"
              (str "Hello " email-name ",\n\n"
                   "This message is to confirm that you have signed up for the Groton Dunstable Elementary Science Fair.\n\n"
                   "We may send you occasional emails regarding the event.\n\n"
                   "The latest info is always available on the website: https://gdesciencefair.org\n\n"
                   "If you wish to stop receiving email notifications, you may do so at this link\n"
                   "    https://gdesciencefair.org/makechanges\n\nScience Fair Team\n")))

(defn md5
  "Generate a md5 checksum for the given string"
  [token]
  (let [hash-bytes
        (doto (java.security.MessageDigest/getInstance "MD5")
          (.reset)
          (.update (.getBytes token)))]
    (.toString
     (new java.math.BigInteger 1 (.digest hash-bytes)) ; Positive and the size of the number
     16)))

(defn make-md5-hash [email]
  (md5 (str (env :secret) email)))

(defn make-email-link [email]
  (if (nil? email) ""
  (str (env :url) "/editreg?h="
       (make-md5-hash email) "&e=" (.replaceAll email "@" "%40"))
  ))

(defn send-make-changes-link [email]
  (let [email-link (make-email-link email)]
    (send-email email email
                "Science Fair Edit Registration"

                (str "\n"
                     "To view/edit your registration, use this link,\n\n"
                     "    " email-link "\n\n"
                     "\n\n"))
    email-link))

