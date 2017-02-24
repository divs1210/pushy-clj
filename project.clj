(defproject pushy-clj "0.3.0"
  :description "A Clojure wrapper over Pushy for sending APNs push notifications."
  :url "https://github.com/divs1210/pushy-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.eclipse.jetty.alpn/alpn-api "1.1.2.v20150522"]
                 [io.netty/netty-tcnative-boringssl-static "1.1.33.Fork17"]
                 [io.netty/netty-all "4.1.0.CR7"]
                 [com.relayrides/pushy "0.9.2"]
                 [org.clojure/data.json "0.2.6"]])
