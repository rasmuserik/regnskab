(ns solsort.regnskab.regnskab
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]]
   [reagent.ratom :as ratom :refer  [reaction]])
  (:require
   [cljs.reader]
   [solsort.toolbox.setup]
   [solsort.toolbox.appdb :refer [db db! db-async!]]
   [solsort.toolbox.ui :refer [input select]]
   [solsort.util
    :refer
    [<ajax <seq<! js-seq load-style! put!close!
     parse-json-or-nil log page-ready render dom->clj]]
   [reagent.core :as reagent :refer []]
   [clojure.string :as string :refer [replace split blank?]]
   [cljs.core.async :refer [>! <! chan put! take! timeout close! pipe]]))

(db! [:entries] {})
(go
  (db!
   [:entries]
   (into
    {}
    (let [entries (map
                   (fn [a] [(get a "_id") a])
                   (js->clj
                    (map
                     js/JSON.parse
                     (.filter
                      (.split
                       (<! (<ajax "regnskab.jsonl" :result :text))
                       "\n")
                      #(not= 0 (.-length %))))))
          duplicates (map first
                          (remove #(= 1 (second %))
                              (frequencies (map first entries))))
          ]
      (log 'dup duplicates)
      entries))))
(defn all-transfers []
   (sort
    (apply
     concat
     (for [entry (vals (db [:entries]))] (get entry "transfers")))))

(defn account-sum [transfers acc log]
  (if (empty? transfers)
    log
    (let [[date from to amount] (first transfers)
          status (-> acc
                     (assoc from (- (get acc from) amount))
                     (assoc to (+ (get acc to) amount)))
          ]
      (when (= from to) (throw "from=to"))
      (recur
       (rest transfers)
       status
       (conj log status)))))


(defn entries []
  (into
   [:div
    {:style
     {:display :inline-block
      :white-space :nowrap
      :height js/window.innerHeight
      :width "50%"
      :overflow :scroll
      }}
    ]
   (for [entry (reverse (sort-by #(get % "date") (vals (db [:entries]))))]
     [:div
      {:on-mouse-over #(db! [:current-entry] (log (get entry "_id")))
       :style
       {:height "4em"
        :border-bottom "1px solid gray"
        :ontouchstart #(db! [:current-entry] (get entry "_id"))
        :overflow :hidden}}
      [:small (get entry "date")]
      " "
      [:big [:strong  (get entry "title")]]
      [:br]
      (str entry)])
   )
  )

(defn current-entry []
  (let [id (db [:current-entry])
        o (db [:entries id])]
   [:div
    {:style
     {:display :inline-block
      :width "50%"
      :vertical-align :top}
     }
    [:div
     [input {:size 10
             :style {:width "30%"}
             :db [:entries id "date"]}]
     [input {:size 20
             :style {:width "70%"}
             :db [:entries id "title"]}]]
    [input {:type :textarea
            :style {:text-align :left
                    :width "100%"}
            :rows 3
            :db [:entries id "notes"]}]
    (into
     [:div]
     (for [i (range (inc (count (get o "transfers"))))]
       [:p {:style {:text-align :center}}
         [input {:size 10
                 :db [:entries id "transfers" i 0]}]
        [input {:style {:width "20%"}
                 :db [:entries id "transfers" i 1]}]
         " \u2192 "
        [input {:style {:width "20%"}
                 :db [:entries id "transfers" i 2]}]
        [input {:style {:width "20%"}
                 :db [:entries id "transfers" i 3]}]]
       )
     )



    (str (db [:entries (db [:current-entry])] ""))]))

(defn accounting-table []
  (let [ledger (account-sum (all-transfers) {} [])
        cols (keys (last ledger))]
    [:div
     (str cols)
     (into
      [:table
       (into [:tr] (for [col cols] [:th col]))]
      (interleave
       (for [line (reverse ledger)]
         [:tr
          {:key (js/Math.random)}
          (for [col cols] [:td {:key col
                                :style {:padding-left "1em"
                                        :text-align :right}}
                           (/ (bit-or 0 (* 100 (get line col 0))) 100)])])
       (for [line (reverse (all-transfers))]
         [:tr
          {:key (str line)}
          [:td
           {:col-span 11}
           (str line)]])
       )
      )]
  ))
(log (map str (sort (all-transfers))))
(defn main []
  [:div
   {:style
    {:vertical-align :top}}
    [entries]
    [current-entry]
   [accounting-table]
    #_(into
     [:div]
     (for [transfer (all-transfers)]
       [:div (str transfer)]))])
(render
 [main])
