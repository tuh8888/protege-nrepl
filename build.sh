## build just the plugin and dump it to protege

export RELEASE_VERSION=0.2.0-SNAPSHOT

./scripts/build-server.sh

cd plugin
mvn -DskipTests=true install
cd ..
./scripts/build-plugin.sh

protege
