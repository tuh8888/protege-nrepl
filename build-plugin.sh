## build just the plugin and dump it to protege

# cd nrepl-clojure
# clojure -X:depstar uberjar :jar target/nrepl-clojure.jar
# mvn install:install-file -Dfile=target/nrepl-clojure.jar -DgroupId=ucdenver.ccp -DartifactId=nrepl-clojure -Dversion=0.2.0-SNAPSHOT -Dpackaging=jar
# cd ..
#
cd nrepl-plugin/
#mvn -DskipTests=true install
cp target/protege-nrepl*.jar /opt/protege/plugins/

protege
#cd /opt/protege
#sh run.sh
