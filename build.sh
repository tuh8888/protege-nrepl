## build just the plugin and dump it to protege
rm /opt/protege/plugins/protege-nrepl*.jar

export RELEASE_VERSION=0.2.0

./scripts/build-server.sh

cd plugin
mvn -DskipTests=true install
cd ..
./scripts/build-plugin.sh

protege
