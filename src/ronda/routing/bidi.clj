(ns ronda.routing.bidi
  (:require [ronda.routing
             [descriptor :as describe]
             [utils :refer [stringify-vals]]]
            [bidi.bidi :as bidi]))

;; ## Analysis

(defn- append-route
  "Concatenate two parts of a bidi route."
  [a b]
  (cond (and (string? a) (string? b)) [(str a b)]
        (and (vector? a) (vector? b)) (reduce conj a b)
        (vector? a) (recur a [b])
        :else (recur [a] b)))

(defn- merge-strings
  "Merge subsequent strings in the given bidi route."
  [route]
  (->> (partition-by string? route)
       (mapcat
         (fn [[a & _ :as v]]
           (if (string? a)
             [(apply str v)]
             v)))
       (vec)))

(defn- unwrap-single-string
  "If the whole route consistes of just a string, unwrap it."
  [[a :as route]]
  (if (= (count route) 1)
    (if (string? a)
      a
      route)
    route))

(defn- attach-route-params-meta
  "Collect the route params in the given bidi route and attach it
   as the metadata key `:route-params`."
  [route]
  (if (string? route)
    route
    (->> (for [route-element route
               :let [route-param (if (vector? route-element)
                                  (peek route-element)
                                  route-element)]
               :when (keyword? route-param)]
           route-param)
         (set)
         (vary-meta route assoc :route-params))))

(defn- remove-empty-segments
  "Remove all empty string segments from the given route."
  [route]
  (vec (remove #{""} route)))

(defn- wrap-method
  "Create ':method/<method>' keyword."
  [method]
  {:pre [(keyword? method)]}
  (keyword "method" (name method)))

(defn- unwrap-method
  "Create method from ':method/<method>' keyword."
  [k]
  {:pre [(= (namespace k) "method")]}
  (keyword (name k)))

(defn- path->map
  [path]
  (-> (if (string? path)
        {:path path}
        (reduce
          (fn [m s]
            (if (and (keyword? s)
                     (= (namespace s) "method"))
              (update-in m [:methods] (fnil conj #{}) (unwrap-method s))
              (update-in m [:path] conj s)))
          {:path []}
          path))
      (update-in [:path] unwrap-single-string)
      (with-meta (meta path))))

(defn- normalize-path
  "Normalize the given path by prefixing it, merging all strings"
  [prefix path]
  (->> (append-route prefix path)
       (merge-strings)
       (remove-empty-segments)
       (unwrap-single-string)
       (attach-route-params-meta)))

(defn- analyze*
  "Analyze the given bidi route spec and produce a map
   of route ID -> flattened route spec."
  [routes]
  (if (vector? routes)
    (let [[prefix spec] routes]
      (->> (for [[id path] (analyze* spec)]
             [id (normalize-path prefix path)])
           (into {})))
    (if (map? routes)
      (->> (for [[k v] routes]
             (analyze*
               (vector
                 (if (keyword? k)
                   (wrap-method k)
                   k)
                 v)))
           (into {}))
      {routes []})))

(defn- analyze
  "Analyze the given bidi route spec and produce a map
   of route ID -> map of `:path` and `:methods`."
  [routes]
  (->> (analyze* routes)
       (map (juxt key (comp path->map val)))
       (into {})))

;; ## Generate

(defn- generate-route-path
  "Generate path for the given route ID."
  [raw-routes route-id values]
  (->> values
       (apply concat)
       (apply bidi/path-for raw-routes route-id)))

(defn- generate-route
  "Generate map of `:path`, `:route-params`, `:query-params`
   for the given route ID."
  [raw-routes analyzed-routes route-id values]
  (let [route-params (-> (get analyzed-routes route-id)
                         (meta)
                         (:route-params))
        params (stringify-vals values)]
    (if-let [path (generate-route-path raw-routes route-id params)]
      {:path  path
       :route-params (select-keys params route-params)
       :query-params (apply dissoc params route-params)}
      (throw
        (ex-info
          (format "unknown route ID: %s" route-id)
          {:route-id route-id
           :values values})))))

;; ## Match

(defn- bidi-match
  "Match bidi route with method."
  [routes method uri]
  (bidi/match-route routes uri :request-method method))

(defn- match-bidi-route
  "Match bidi route with method, return map with `:id`
   and `:route-params`."
  [routes method uri]
  (if-let [m (bidi-match routes method uri)]
    {:id           (:handler m)
     :route-params (into {} (:route-params m))}))

(defn- prefix-bidi-route
  "Prefix the given bidi route spec."
  [routes v]
  {:pre [(= (count routes) 2)]}
  [v {(first routes) (second routes)}])

;; ## Descriptor

(deftype BidiDescriptor [raw-routes analyzed-routes]
  describe/RouteDescriptor
  (match [_ request-method uri]
    (match-bidi-route
      raw-routes
      request-method
      uri))
  (generate [_ route-id values]
    (generate-route
      raw-routes
      analyzed-routes
      route-id
      values))
  (prefix-string [_ s]
    (let [new-routes (prefix-bidi-route raw-routes s)]
      (BidiDescriptor.
        new-routes
        (analyze new-routes))))
  (prefix-route-param [_ k pattern]
    (let [new-routes (->> (if pattern
                            [pattern k]
                            k)
                          (vector)
                          (prefix-bidi-route raw-routes))]
      (BidiDescriptor.
        new-routes
        (analyze new-routes))))
  (routes [_]
    analyzed-routes))

(defn descriptor
  "Create RouteDescriptor based on bidi routes."
  [routes]
  {:pre [(vector? routes)
         (= (count routes) 2)]}
  (BidiDescriptor.
    ;; should be: (bidi/compile-route routes), but juxt/bidi#17
    routes
    (analyze routes)))
