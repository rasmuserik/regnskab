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
  (reverse
   (sort
    (apply
     concat
     (for [entry (db [:entries])] (get entry "transfers")))))
  )

(defn account-sum [transfers acc]
  (if (empty? transfers)
    acc
    (let [[date from to amount] (first transfers)]
      (when (= from to) (throw "from=to"))
      (recur
       (rest transfers)
       (-> acc
           (assoc from (- (get acc from) amount))
           (assoc to (+ (get acc to) amount)))))))

(defn main []
  (into
   [:div
    (str (account-sum (all-transfers) {}))
    ]
   (for [transfer (all-transfers)]
     [:div (str transfer)]))
  )
(render
 [main])
