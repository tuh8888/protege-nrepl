## build just the plugin and dump it to protege

./scripts/build-clojure-nrepl.sh

cd nrepl-plugin/
mvn -DskipTests=true install
./scripts/build-plugin.sh

protege

