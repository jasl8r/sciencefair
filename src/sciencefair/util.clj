(ns sciencefair.util
  (:require [noir.io :as io]
            [noir.session]
            [markdown.core :as md]))

(import 'org.apache.commons.mail.SimpleEmail)

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
  (->>
   (io/slurp-resource filename)
   (md/md-to-html-string)))

(defn dev-mode? []
  (or
   (= "rherrmann-mbpr.local" (.getHostName (java.net.InetAddress/getLocalHost)))
   (= "wilddog" (.getHostName (java.net.InetAddress/getLocalHost)))))

(defn get-smtp-pass []
  (.trim (slurp "/fair-data/smtppass.txt")))

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
    (.setHostName "smtp.sendgrid.net")
    (.setSslSmtpPort "465")
    (.setSSL true)
    (.addTo email-addr email-name)
    (.setFrom "gdesciencefair@gmail.com" "GD Science Fair")
    (.setSubject subject)
    (.setMsg message)
    (.setAuthentication "bherrmann7" (get-smtp-pass))
    (.send)))

(defn send-email-confirmation [email-addr email-name]
  (send-email email-addr email-name
              "Science Fair Signup Confirmation"
              (str "Hello " email-name ",\n\n"
                   "This message is to confirm that you have signed up for emails.\n\n"
                   "We will send out occasional emails about the Groton Dunstatable Elementary Sciene Fair.\n\n"
                   "The latest info is always at the site, http://gdesciencefair.org\n\n"
                   "If you wish to stop receiving email notifications, do so at this link\n"
                   "    http://gdesciencefair.org/makechanges\n\nScience Fair Team\n")))

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
  (md5 (str (.trim (slurp "/fair-data/md5phrase.txt")) email)))

(defn send-make-changes-link [email]
  (let [email-link (str "http://" (if (dev-mode?) "localhost:3000" "gdesciencefair.org") "/editreg?h="
                        (make-md5-hash email) "&e=" (.replaceAll email "@" "%40"))]
    (send-email email email
                "Science Fair Edit Registration"

                (str "\n"
                     "To view/edit your registration, use this link,\n\n"
                     "    " email-link "\n\n"
                     "\n\n"))))

