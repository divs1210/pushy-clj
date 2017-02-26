(defproject pushy-clj "0.3.3"
  :description "A Clojure wrapper over Pushy for sending APNs push notifications."
  :url "https://github.com/divs1210/pushy-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.eclipse.jetty.alpn/alpn-api "1.1.3.v20160715"]
                 [io.netty/netty-tcnative-boringssl-static "1.1.33.Fork24"]
                 [io.netty/netty-all "4.1.6.Final"]
                 [com.relayrides/pushy "0.9.2"]
                 [org.clojure/data.json "0.2.6"]])
