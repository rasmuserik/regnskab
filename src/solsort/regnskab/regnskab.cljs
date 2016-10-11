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

(db! [:entries] [])
(go
  (db! [:entries]
   (js->clj
    (map
     js/JSON.parse
     (.filter
      (.split
       (<! (<ajax "regnskab.jsonl" :result :text))
       "\n")
      #(not= 0 (.-length %)))))
   )
  )
(defn all-transfers []
   (sort
    (apply
     concat
     (for [entry (db [:entries])] (get entry "transfers")))))

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
       (conj log status)
       ))))

(defn main []
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
     )
    (into
     [:div]
     (for [transfer (all-transfers)]
       [:div (str transfer)]))])
  )
(render
 [main])
