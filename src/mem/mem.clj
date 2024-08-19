^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns mem.mem
  (:require [nextjournal.clerk :as clerk]
            [clojure.core.async :as a]))

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

;; stolen from https://stackoverflow.com/questions/26213464/what-is-the-best-way-to-measure-how-much-memory-a-clojure-program-uses
(defn mem-snapshot []
  (float (/ (- (-> (java.lang.Runtime/getRuntime)
                   (.totalMemory))
               (-> (java.lang.Runtime/getRuntime)
                   (.freeMemory)))
            1024)))

;; Lets define a macro that can profile a form
(defmacro profile [body]
  `(let [stop-chn# (a/chan)
         go-chn# (a/go-loop [snapshots# []]
                   (a/alt!
                     (a/timeout 200) (do (println "step!") (recur (concat snapshots# [(mem-snapshot)])))
                     stop-chn# snapshots#))
         res# ~body]
     (a/>!! stop-chn# :stop)
     (a/close! stop-chn#)
     [res# (a/<!! go-chn#)]))

;; Lets try a macroexpand...
(macroexpand '(profile (do (Thread/sleep 100) :result)))

(clerk/plotly {:data [{:x [1 2 3] :y [3 4 5] :type "scatter"}]})

(def res (profile (do (Thread/sleep 5000) :result)))
(clerk/plotly {:data [{:x (range (count (second res)))
                       :y (second res)
                       :type "scatter"}]})
