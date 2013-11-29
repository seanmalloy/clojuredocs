(ns clojuredocs.main
  (:require [ring.adapter.jetty :as jetty]
            [aleph.http :as ah]
            [clojuredocs.env :as env]
            [clojuredocs.entry :as entry]))

(defn start-http-server [entry-point opts]
  (ah/start-http-server
    (ah/wrap-ring-handler
      (fn [r]
        (let [resp (entry-point r)]
          (if (:status resp)
            resp
            (assoc resp :status 200)))))
    opts))

(defn -main []
  (let [port (env/int :port 8080)]
    (start-http-server
      (var entry/routes)
      {:port port :join? false})
    (println (format "Server running on port %d" port))))