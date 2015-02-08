

(require  '[markdown.core :as md])

(defn template-wrap [x]
  (str "{% extends \"sciencefair/views/templates/base.html\" %}
  {% block content %} "
  x
  "\n{% endblock %}\n"))


(spit "src/sciencefair/views/templates/rules.html"  (template-wrap (md/md-to-html-string (slurp "rules.md"))))

(spit "src/sciencefair/views/templates/info.html"   (template-wrap (md/md-to-html-string (slurp "info.md"))))

(println "Markdown updated.")