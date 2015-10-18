(ns gravity.events
  (:require-macros
   [gravity.macros :refer [λ]]
   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! chan]]
            [gravity.tools :refer [log]]))



(defn create-chan
  "Return a simple chan
  - avoiding a :require into core
  - centralizing the creation"
  []
  (chan 1024))


(defn create-store
  "Create an atom and two closures ':on' and ':get-callbacks'.
  - Use :on to store a callback
  - Use :get-callbacks to retrive a deref of the callbacks atom map.
  Warning : the keys are transformed to keywords."
  []
  (let [callbacks-map (atom {})]

    ;;return 'on'
    {:on (λ [cb-key callback]
            (swap! callbacks-map assoc (keyword cb-key) callback))
     :off (fn
            ([]
             (reset! callbacks-map {}))
            ([cb-key]
             (swap! callbacks-map assoc (keyword cb-key) nil)) )

     :get-callbacks (λ [] @callbacks-map)}))




;; Events triggers

(defn- get-callbacks
  "Extract and execute the :get-callbacks closure from the store"
  [store]
  (let [deref-callbacks (:get-callbacks store)]
    (deref-callbacks)))

(defn- trigger
  [callback & args]
  (when-not (nil? callback)
    (apply callback args)))


(defn- trigger-nodeover
  "If the mouse hovered a node"
  [event state store]
  (let [store (get-callbacks store)
        callback (:nodeover store)]
    (when (nil? (:target @state))
      (swap! state assoc :target (:target event))
      (trigger callback (:target event)))))

(defn- trigger-select-node
  "If the mouse click a node"
  [event state store]
  (let [store (get-callbacks store)
        callback (:nodeselect store)]
    (trigger callback (:target event))))


(defn- trigger-nodeblur
  "If the mouse hovered a node, trigger call the callback"
  [event state store]
  (let [store (get-callbacks store)
        callback (:nodeblur store)]
    (when-not (nil? (:target @state))
      (swap! state assoc :target nil)
      (trigger callback))))




;; dispatcher

(defn listen
  "Listen to a chan and trigger the appropriate callbacks if found in the events-store"
  [chan store]
  (let [state (atom {:target nil
                     :selected nil})]
    (go
     (while true
       (let [event (<! chan)]
         (case (:type event)
           :mouse-in-node (trigger-nodeover event state store)
           :mouse-out-node (trigger-nodeblur event state store)
           :select-node (trigger-select-node event state store)
           nil)
         )))))





