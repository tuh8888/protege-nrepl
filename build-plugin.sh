## build just the plugin and dump it to protege

#cd nrepl-clojure
#lein uberjar
#mvn install:install-file -Dfile=target/nrepl-clojure-0.2.0-SNAPSHOT-standalone.jar -DgroupId=ucdenver.ccp -DartifactId=nrepl-clojure -Dversion=0.2.0-SNAPSHOT-standalone -Dpackaging=jar

cd nrepl-plugin/
mvn -DskipTests=true install
cp target/protege-nrepl*.jar /opt/protege/plugins/

#cd /opt/protege
#sh run.sh
