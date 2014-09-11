(ns clojuredocs.mods.styleguide
  (:require [dommy.core :as dommy]
            [clojure.string :as str]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts! timeout pipe mult tap]]
            [om.core :as om :include-macros true]
            [om.dom :as omdom]
            [sablono.core :as sab :refer-macros [html]]
            [clojuredocs.examples :as examples]
            [cljs.reader :as reader])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [dommy.macros :refer [node sel sel1]]))


;; Inspectable react widgets -- internal state & egress

#_[:ul.nav.nav-tabs
   [:li
    {:class (when (= :editor active) "active")}
    [:a {:href "#"
         :on-click #(do
                      (om/set-state! owner :active :editor)
                      false)}
     "Widget"]]
   [:li
    {:class (when (= :preview active) "active")}
    [:a {:href "#"
         :on-click #(do
                      (om/set-state! owner :active :preview)
                      false)}
     ""]]]

(defn $inspector [$widget]
  (fn [app owner]
    (reify
      om/IInitState
      (init-state [_]
        {:text (pr-str app)})
      om/IWillMount
      (will-mount [_]
        (let [outlets (om/get-state owner :outlets)]
          (doseq [[k c] outlets]
            (go-loop []
              (when-let [res (<! c)]
                (om/update-state! owner
                  (fn [m]
                    (update-in m [:messages] concat [[k res]])))
                (recur))))))
      om/IRenderState
      (render-state [_ {:keys [active text error? outlets messages] :as state}]
        (html
          [:div.sg-example-inspector
           [:div.row
            [:div.col-sm-5
             [:div.sg-inspector-state
              [:h5 "State"]
              (let [rows (Math/max
                           (+ (->> text
                                   (filter #(= "\n" %))
                                   count)
                             3)
                           5)]
                [:textarea
                 {:class (when error? "error")
                  :value text
                  :on-change #(do
                                (prn "hi")
                                (let [new-text (.. % -target -value)]
                                  (om/set-state! owner :text new-text))
                                (try
                                  (om/transact! app
                                    (fn [_]
                                      (reader/read-string (.. % -target -value))))
                                  (om/set-state! owner :error? false)
                                  (catch js/Error e
                                    (om/set-state! owner :error? true)
                                    (prn e))))
                  :rows rows}])]
             [:div.sg-inspector-outlets
              [:div.pull-right
               [:a.clear {:href "#"
                          :on-click #(do
                                       (om/set-state! owner :messages [])
                                       false)}
                "clear"]]
              [:h5 "Outlets"]
              (for [[source message :as p] messages]
                [:div.message
                 [:h6 (name source)]
                 [:pre (pr-str message)]])]]
            [:div.col-sm-7
             [:div.sg-example
              [:div.checker-bg
               (om/build $widget app {:init-state outlets})]]]]])))))

(defn root-sel [selector widget app-state & [opts]]
  (doseq [$el (sel selector)]
    (om/root
      widget
      app-state
      (merge opts {:target $el}))))

(def ex-example-0
  {:body "user=> (map #(vector (first %) (* 2 (second %)))
            {:a 1 :b 2 :c 3})

([:a 2] [:b 4] [:c 6])

user=> (into {} *1)
{:a 2, :b 4, :c 6}"
   :author {:login "zk" :account-source "github" :avatar-url "https://avatars.githubusercontent.com/u/7194?v=2"}})


(def ex-example-1
  {:body "user=> (map #(vector (first %) (* 2 (second %)))
            {:a 1 :b 2 :c 3})

([:a 2] [:b 4] [:c 6])

user=> (into {} *1)
{:a 2, :b 4, :c 6}"
   :author {:login "zk"
            :account-source "github"
            :avatar-url "https://avatars.githubusercontent.com/u/7194?v=2"}
   :editors [{:login "masondesu"
              :account-source "github"
              :avatar-url "http://www.gravatar.com/avatar/091bc95204caaf52b0d299bd9ac59540?s=48&d=identicon"}
             {:login "dakrone"
              :account-source "github"
              :avatar-url "http://www.gravatar.com/avatar/bcacd00a7f05c4772329cf9f446c7987?s=48&d=identicon"}]})


(def ex-example-2
  {:body "user=> (map #(vector (first %) (* 2 (second %)))
            {:a 1 :b 2 :c 3})

([:a 2] [:b 4] [:c 6])

user=> (into {} *1)
{:a 2, :b 4, :c 6}"
   :user {:email "zachary.kim@gmail.com"}
   :editors [{:avatar-url "https://avatars.githubusercontent.com/u/7194?v=2"}
             {:email "foo@barrrrrrrrr.com"}
             {:email "foo@barrrrrrr.com"}
             {:email "foo@barrrrrr.com"}
             {:email "foo@barrrrr.com"}
             {:email "foo@barrrr.com"}
             {:email "foo@barrr.com"}
             {:email "foo@barr.com"}
             {:email "foo@bar.com"}]})

(defn init [$root]
  (root-sel
    :.sg-tabbed-clojure-editor
    examples/$tabbed-clojure-editor
    {})

  #_(root-sel
    :.sg-tabbed-markdown-editor
    examples/$tabbed-markdown-editor
    {})

  (root-sel
    :.sg-examples-null-state
    examples/$examples
    {:var {:name "bar" :ns "foo"}
     :add-example {}})

  (root-sel
    :.sg-examples-single
    examples/$examples
    {:examples [ex-example-0]
     :var {:name "bar" :ns "foo"}})

  (root-sel
    :.sg-examples-lengths
    examples/$examples
    {:examples [ex-example-0
                ex-example-1
                ex-example-2]
     :var {:name "bar" :ns "foo"}})

  (root-sel
    :.sg-add-example
    examples/$create-example
    {:editing? true})

  (root-sel
    :.sg-add-example-loading
    examples/$create-example
    {:editing? true
     :loading? true
     :body "(defn greet [name]\n  (println \"Hello\" name))"})


  (root-sel
    :.sg-add-example-errors
    examples/$create-example
    {:body "(defn greet [name]\n  (println \"Hello\" name))"
     :error "This is where error messages that apply to the whole form go. And here's some other text to show what happens with a very long error message."
     :create-success? true
     :var {:ns "clojure.core"
           :name "foo"}
     :editing? true})

  (root-sel
    :.sg-edit-example
    examples/$example
    (merge
      ex-example-0
      {:editing? true
       :can-delete? true}))

  (root-sel
    :.sg-delete-example
    examples/$example-meta
    {:body "user=> (foo)"
     :author {:login "zk" :account-source "github" :avatar-url "https://avatars.githubusercontent.com/u/7194?v=2"}
     :history [{:author {:login "dakrone" :account-source "github" :avatar-url "https://avatars3.githubusercontent.com/u/19060?v=2&s=460"}}]
     :can-delete? true})

  (root-sel
    :.sg-delete-example-confirm
    examples/$example-meta
    {:body "user=> (foo)"
     :author {:login "zk" :account-source "github" :avatar-url "https://avatars.githubusercontent.com/u/7194?v=2"}
     :history [{:author {:login "dakrone" :account-source "github" :avatar-url "https://avatars3.githubusercontent.com/u/19060?v=2&s=460"}}]
     :can-delete? true
     :delete-state :confirm})

  (root-sel
    :.sg-delete-example-loading
    examples/$example-meta
    {:body "user=> (foo)"
     :author {:login "zk" :account-source "github" :avatar-url "https://avatars.githubusercontent.com/u/7194?v=2"}
     :history [{:author {:login "dakrone" :account-source "github" :avatar-url "https://avatars3.githubusercontent.com/u/19060?v=2&s=460"}}]
     :can-delete? true
     :delete-state :loading})

  (root-sel
    :.sg-delete-example-error
    examples/$example-meta
    {:body "user=> (foo)"
     :author {:login "zk" :account-source "github" :avatar-url "https://avatars.githubusercontent.com/u/7194?v=2"}
     :history [{:author {:login "dakrone" :account-source "github" :avatar-url "https://avatars3.githubusercontent.com/u/19060?v=2&s=460"}}]
     :can-delete? true
     :delete-state :error}))


#_(fn [$el]
    (let [ch (chan)]
      (go-loop []
        (when-let [ex (<! ch)]
          (prn ex)
          (recur)))
      (om/root
        examples/$create-example
        {}
        {:target $el
         :init-state {:new-example-ch ch}})))
