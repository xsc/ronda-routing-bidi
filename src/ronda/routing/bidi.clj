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

(defn- attach-existing-meta
  [existing routes]
  (for [[route-id m] routes
        :let [mta (get-in existing [route-id :meta])]]
    [route-id (if mta (assoc m :meta mta) m)]))

(defn- analyze
  "Analyze the given bidi route spec and produce a map
   of route ID -> map of `:path` and `:methods`."
  [routes & [existing]]
  (->> (analyze* routes)
       (map (juxt key (comp path->map val)))
       (attach-existing-meta existing)
       (into {})))

;; ## Generate

(defn- generate-route-path
  "Generate path for the given route ID."
  [raw-routes route-id values]
  (->> values
       (apply concat)
       (apply bidi/path-for raw-routes route-id)))

(defn- generate-route
  "Generate map of `:path`, `:route-params`, `:query-params` and `:meta`
   for the given route ID."
  [raw-routes analyzed-routes route-id values]
  (let [{mta :meta :as data} (get analyzed-routes route-id)
        route-params (-> data meta :route-params)
        params (stringify-vals values)]
    (if-let [path (generate-route-path raw-routes route-id params)]
      (cond-> {:path  path
               :route-params (select-keys params route-params)
               :query-params (reduce dissoc params route-params)}
        mta (assoc :meta mta))
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
  [compiled-routes analyzed-routes method uri]
  (if-let [{:keys [handler route-params]}
           (bidi-match compiled-routes method uri)]
    (let [mta (get-in analyzed-routes [handler :meta])]
      (cond-> {:id handler
               :route-params (into {} route-params)}
        mta (assoc :meta mta)))))

(defn- prefix-bidi-route
  "Prefix the given bidi route spec."
  [routes v]
  {:pre [(= (count routes) 2)]}
  [v {(first routes) (second routes)}])

;; ## Descriptor

(deftype BidiDescriptor [raw-routes
                         compiled-routes
                         analyzed-routes]
  describe/RouteDescriptor
  (match [_ request-method uri]
    (match-bidi-route
      compiled-routes
      analyzed-routes
      request-method
      uri))
  (generate [_ route-id values]
    (generate-route
      raw-routes
      analyzed-routes
      route-id
      values))
  (update-metadata [this route-id f]
    (->> (if (contains? analyzed-routes route-id)
           (update-in analyzed-routes [route-id :meta] f)
           (throw
             (IllegalArgumentException.
               (format "no such route: %s" route-id))))
         (BidiDescriptor. raw-routes compiled-routes)))
  (routes [_]
    analyzed-routes)

  describe/PrefixableRouteDescriptor
  (prefix-string [_ s]
    (let [new-routes (prefix-bidi-route raw-routes s)]
      (BidiDescriptor.
        new-routes
        (bidi/compile-route new-routes)
        (analyze new-routes analyzed-routes))))
  (prefix-route-param [_ k pattern]
    (let [new-routes (->> (if pattern
                            [pattern k]
                            k)
                          (vector)
                          (prefix-bidi-route raw-routes))]
      (BidiDescriptor.
        new-routes
        (bidi/compile-route new-routes)
        (analyze new-routes analyzed-routes)))))

(defn descriptor
  "Create RouteDescriptor based on bidi routes."
  [routes]
  {:pre [(vector? routes)
         (= (count routes) 2)]}
  (BidiDescriptor.
    routes
    (bidi/compile-route routes)
    (analyze routes)))
