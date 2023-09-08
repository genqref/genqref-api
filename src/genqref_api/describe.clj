(ns genqref-api.describe
  "Just a namespace to store all the descriptions.")

;; TODO: this could also come from an org-file

(def Markets
  "<p>Please use this endpoint to report observations of markets. It
  takes a list of markets as recieved by the <code>get-market</code>
  endpoint of the <i>SpaceTraders API</i>. Please consult the Model
  below to see what properties are required and which are optional.
  <b>It is encouraged to only send required properties.</b> Optional
  properties will be accepted but in most cases discarded for
  brevity.</p>

  <p>Transactions are optional. Please use the dedicated endpoint to
  report observed transactions.</p>")
