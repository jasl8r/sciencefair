
(def m { :grade "one", :title "bblblbl" })

(defn k [map-set seq postfix]
  (if (empty? seq)
    map-set
    (let [[k v] (first seq)]
    (recur (conj map-set { (keyword (str (name k) postfix)) v }) (rest seq) postfix)
    )
  )
  )

(prn (k {} m 2))
