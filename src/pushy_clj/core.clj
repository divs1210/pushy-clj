(ns ^{:author "Divyansh Prakash <divyansh@helpshift.com>"
      :doc "Wrapper over Pushy APNs lib <https://github.com/relayrides/pushy>"}
  pushy-clj.core
  (:require [clojure.data.json :as json])
  (:import [com.relayrides.pushy.apns ApnsClient ApnsPushNotification]
           [com.relayrides.pushy.apns.util SimpleApnsPushNotification TokenUtil]
           io.netty.util.concurrent.Future
           java.io.InputStream
           [java.util.concurrent TimeoutException TimeUnit]))

(def ^:const apns-hosts
  {:dev  ApnsClient/DEVELOPMENT_APNS_HOST
   :prod ApnsClient/PRODUCTION_APNS_HOST})


(defn ^ApnsClient make-client
  "Takes the certificate as an InputStream and a password
  and returns an ApnsClient."
  [^InputStream cert ^String pass]
  (ApnsClient. cert pass))


(defn connect
  "Establishes an APNs connection.
  `host` must be either :dev or :prod."
  [^ApnsClient client host]
  (.await (.connect client (apns-hosts host))))


(defn disconnect
  "Closes the APNs connection."
  [^ApnsClient client]
  (.await (.disconnect client)))


(defn build-payload
  "Takes a APNs payload as a hashmap, converts it into JSON,
  and barfs if size exceeds limit-size."
  [payload-map & {:keys [limit-size] :or {limit-size 4096}}]
  (let [payload (json/write-str payload-map)]
    (if (> (count payload) limit-size)
      (throw (IllegalArgumentException. "Payload exceeded limit-size!"))
      payload)))


(defn ^SimpleApnsPushNotification build-push-notification
  "Returns a SimpleApnsPushNotification object.
  `token` is the device token
  `topic` if nil, will be picked from the cert
  `payload` should be a hashmap that follows Apple's guidelines:  http://tinyurl.com/jj97ep6"
  [^String token ^String topic payload]
  (SimpleApnsPushNotification. (TokenUtil/sanitizeTokenString token)
                               topic
                               (build-payload payload)))


(defn ^:private response->map
  "Converts a PushNotificationResponse into a hashmap with three keys:
  `:accepted?` whether the notification was accepted by APNs
  `:rejection-reason` why the notification was rejected (if it was)
  `:token-expiration-ts` when the token expired (if it did)"
  [response]
  (try
    {:accepted? (.isAccepted response)
     :rejection-reason (.getRejectionReason response)
     :token-expiration-ts (.getTokenInvalidationTimestamp response)}
    (catch Exception e
      {:accepted? false
       :rejection-reason "ConnectionError"
       :token-expiration-ts nil})))


(defn send-push-notification
  "Sends the given notification asynchronously.
  Returns a future  that can be derefed (using deref/@) to get the
  response synchronously as a map with three keys:
  `:accepted?` whether the notification was accepted by APNs
  `:rejection-reason` why the notification was rejected (if it was)
  `:token-expiration-ts` when the token expired (if it did)"
  [^ApnsClient client ^ApnsPushNotification notification]
  (let [response-future (.sendNotification client notification)]
    (reify
      clojure.lang.IDeref
      (deref [_]
        (response->map (.get response-future)))
      clojure.lang.IBlockingDeref
      (deref [_ timeout-ms timeout-val]
        (try (response->map (.get response-future timeout-ms TimeUnit/MILLISECONDS))
             (catch TimeoutException e
               timeout-val)))
      clojure.lang.IPending
      (isRealized [_]
        (.isDone response-future))
      java.util.concurrent.Future
      (get [_]
        (response->map (.get response-future)))
      (get [_ timeout unit]
        (response->map (.get response-future timeout unit)))
      (isCancelled [_]
        (.isCancelled response-future))
      (isDone [_]
        (.isDone response-future))
      (cancel [_ interrupt?]
        (.cancel response-future interrupt?)))))
