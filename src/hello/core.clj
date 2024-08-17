^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns hello.core
  (:require [nextjournal.clerk :as clerk]))

^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(defn clerk-start! []
  (let [clerk-serve (requiring-resolve 'nextjournal.clerk/serve!)
        clerk-port 7806]
    (clerk-serve {:browse? true :port clerk-port})))

^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(comment
  (clerk-start!)
  (clerk/build! {:paths ["src/hello/core.clj"]}))

;; This is just a test notebook
