# pushy-clj

A Clojure library for sending push notifications via APNS to Apple devices
using the new HTTP/2 protocol.

It's a thin wrapper around [Pushy](https://github.com/relayrides/pushy).

Make sure you have one of:
* [Universal Push Notification Client SSL Certificate](https://developer.apple.com/library/ios/documentation/IDEs/Conceptual/AppDistributionGuide/AddingCapabilities/AddingCapabilities.html#//apple_ref/doc/uid/TP40012582-CH26-SW11)
* APNS Auth Key from [Apple member center](https://developer.apple.com/account/)

## Leiningen

`[pushy-clj "0.3.2"]`

## Usage

First, we create a client and connect to the APNs development server:

```clojure
(require '[pushy-clj.core :refer :all]
         '[clojure.java.io :as io])

(import 'java.io.File)

;; Old-school certificate-based auth
(with-open [cert (io/input-stream (File. "/path/to/cert.p12"))]
  (def client (make-client cert "password")))

;; New token-based auth
(with-open [key (io/input-stream (File. "/path/to/key.p8"))]
  (def client (make-client key "team-id" "key-id" ["topic-1" "topic-2"])))

(connect client :dev) ;; blocking operation, unlike Pushy
                      ;; use :prod in production env
```

Then we build a notification following Apple's [guidelines](https://developer.apple.com/library/content/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/CreatingtheNotificationPayload.html#//apple_ref/doc/uid/TP40008194-CH10-SW1):

```clojure
(def payload {:aps {:alert "Hello!"}})

(def notification (build-push-notification "device-token" 
                                           "topic" ;; this can be nil
                                           payload))
```

Now, we can send the notification:

```clojure
(def resp-future (send-push-notification client notification)) ;; async operation!
```

The notification is sent asynchronously, and `resp-future` is returned immediately.
This future can be derefed to get the response as a hashmap.

```clojure
@resp-future ;; blocking operation

;; => {:accepted? true
;;     :rejection-reason nil
;;     :token-expiration-ts nil}
```

Finally, we can close the connection using `disconnect`:

```clojure
(disconnect client) ;; blocking operation
```

Apple considers frequent connecting and disconnecting to be a DoS attack, so
be sure to re-use existing connections.

## License

Copyright © 2016 Divyansh Prakash

Distributed under the Eclipse Public License version 1.0.
