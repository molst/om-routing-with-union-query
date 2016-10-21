# om.next routing with union queries

# Get started
 * boot local-dev watch reload cljs start
 * load http://localhost:4410/index.html in browser
 * watch the console output

# The question

When running in remote-mode, the parser finds no query in the ast for the key :a-view. Why is that? If the query in AView is simplified to just contain a keyword there will be a query present.
