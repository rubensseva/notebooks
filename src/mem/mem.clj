^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns mem.mem
  (:require
   [clojure.core.async :as a]
   [clojure.reflect :as reflect]
   [nextjournal.clerk :as clerk]))

^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(defn clerk-start! []
  (let [clerk-serve (requiring-resolve 'nextjournal.clerk/serve!)
        clerk-port 7806]
    (clerk-serve {:browse? true :port clerk-port})))

^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(comment
  (clerk-start!)
  (clerk/build! {:paths ["src/mem/mem.clj"]}))

;; # Profiling memory usage the naive way

;; I have no idea how to profile clojure code with regards to memory usage.  I've
;; seen some tools mentioned in the wild, but what I want is something dead
;; simple that just works without any fancy setup. I know we can get some data
;; on the amount of free memory from the java Runtime object, so lets experiment
;; with that

(def r (java.lang.Runtime/getRuntime))

;; lets see what we are dealing with here...
(->> (:members (reflect/reflect r))
     (map :name))

;; Alright so regarding memory, these methods seems relevant:
(.totalMemory r)
(.freeMemory r)
(.maxMemory r)

;; Pretty nice! I assume these are in bytes, so let's get the value in a more
;; human readable format
(defn human-bytes [b]
  (if (> b 1e9)
    (format "%.2fGB" (/ b (float 1e9)))
    (format "%.2fMB" (/ b (float 1e6)))))
(human-bytes (.totalMemory r))
(human-bytes (* (.totalMemory r) 100))

;; To get the current amount of used memory we should be able to subtract the
;; amount of free memory from the total memory
(defn usedMemory [r]
  (- (.totalMemory r) (.freeMemory r)))

(human-bytes (usedMemory r))

;; Alright so we have a nice way of getting the current amount of used memory.
;; Next on the agenda is to find some way of profiling a form. I think a macro
;; should fit nicely for this. The idea would be a macro that can take any form,
;; continuosly reads the amount of used memory while evaluating the form, then
;; return the result of the form and the memory usage information

(defmacro profile [body]
  `(let [stop-chn# (a/chan)
         go-chn# (a/go-loop [snapshots# []]
                   (a/alt!
                     (a/timeout 100) (recur (concat snapshots# [(usedMemory r)]))
                     stop-chn# snapshots#))
         res# ~body]
     (Thread/sleep 500)
     (a/>!! stop-chn# :stop)
     (a/close! stop-chn#)
     [res# (a/<!! go-chn#)]))

;; That should work nicely. The profile macro starts a background go block that
;; gathers the profile information. When the input form is evaluated, the go
;; block is stopped, and the results from the form and the profiling is returned

;; Lets try it out with a macroexpand and see what we get
(clerk/code (macroexpand '(profile (do (Thread/sleep 1000) :result))))

;; Looks alright, lets try to execute the macro
(profile (do (Thread/sleep 1000) "result"))

;; Looks good!

;; In order to test it properly, we need to actually use some memory for this to work. This is a bit of an
;; ungodly function, but hopefully it does the job

(defn create-random []
  (loop [current {}]
    (let [new (merge current {(str (rand-int 999999)) (str (rand-int 999999))})]
      (if (>= (rand-int 10) 9)
        new
        (recur new)))))

;; Lets try it out
(repeatedly 10 create-random)

;; Alright, with some profiling this time
(def result (profile (doall (repeatedly 1000000 create-random))))

;; We should be able to use this data to create a plot

;; need a little helper function for millisecond labels on the x axis
(defn millis-range [x]
  (loop [i 0
         ms [0]]
    (if (= i x)
      ms
      (recur (inc i) (conj ms (+ (last ms) 200))))))

(clerk/plotly {:data [{:x (millis-range (count (second result)))
                       :y (second result)
                       :type "scatter"}]})
