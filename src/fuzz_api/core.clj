(ns fuzz-api.core
  (:require [clojure.test.check.generators :as gen]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]
            [cheshire.core :refer [generate-string]]))

(def ids (gen/choose 0 1000000))

(def random-items
  (gen/fmap
    (fn [id]
      {:id id})
    ids))

(defn random-list [generator]
  (gen/bind
    (gen/choose 0 50)
    (fn [n]
      (gen/vector generator n))))

(defn item [req]
  {:body (generate-string (gen/generate random-items))})

(defn items [req]
  {:body (generate-string (gen/generate (random-list random-items)))})

(defn handler [req]
  (let [f (condp re-matches (req :uri)
            #"^/api/items/\d+$" item
            #"^/api/items$" items
            (fn [_] {:status 404}))]
    (f req)))

(def app
  (-> handler
    wrap-reload
    wrap-stacktrace))

(defonce server
  (run-jetty #'app {:port 5003 :join? false}))
