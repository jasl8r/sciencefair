

(require  '[markdown.core :as md])

(spit "src/sciencefair/views/templates/rules.html"  (md/md-to-html-string (slurp "rules.md")))

(spit "src/sciencefair/views/templates/info.html"  (md/md-to-html-string (slurp "info.md")))

