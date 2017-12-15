(ns ^{:author "Divyansh Prakash <divyansh@helpshift.com>"
      :doc "Wrapper over Pushy APNs lib <https://github.com/relayrides/pushy>"}
 pushy-clj.core
  (:require [clojure.data.json :as json])
  (:import [com.turo.pushy.apns ApnsClient ApnsClientBuilder ApnsPushNotification PushNotificationResponse]
           [com.turo.pushy.apns.util SimpleApnsPushNotification TokenUtil]
           [com.turo.pushy.apns.auth ApnsSigningKey]
           io.netty.util.concurrent.Future
           java.io.InputStream
           java.util.Collection
           [java.util.concurrent TimeoutException TimeUnit]))

(def ^:const apns-hosts
  {:dev  ApnsClientBuilder/DEVELOPMENT_APNS_HOST
   :prod ApnsClientBuilder/PRODUCTION_APNS_HOST})


(defn ^ApnsClient make-client
  "Builds and and returns an `ApnsClient` that talks to the specified
  `host` (which must be either `:dev` or `:prod`). Create a client
  with a signing key instead of a certificate to use token-based
  authentication."
  ([host ^InputStream cert ^String pass]
   (-> (ApnsClientBuilder.)
       (.setApnsServer (apns-hosts host))
       (.setClientCredentials cert pass)
       .build))
  ([host ^InputStream signing-key-stream ^String team-id ^String key-id]
   (let [signing-key (ApnsSigningKey/loadFromInputStream signing-key-stream team-id key-id)]
     (-> (ApnsClientBuilder.)
         (.setApnsServer (apns-hosts host))
         (.setSigningKey signing-key)
         .build))))

(defn disconnect
  "Closes the APNs connection of the specified client."
  [^ApnsClient client]
  (.await (.close client)))


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
  [^PushNotificationResponse response]
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
  (let [response-future ^Future (.sendNotification client notification)]
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
