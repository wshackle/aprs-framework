#!/bin/sh

#APRSFRAMEWORKRUN.sh
if test ! -f ./run.sh || grep -v '#APRSFRAMEWORKRUN.sh'  ./run.sh  ; then 
    cd "${0%%run.sh}";
fi

if test "x" != "x${JAVA_HOME}" ; then
    export PATH="${JAVA_HOME}/bin/:${PATH}";
fi

export JARFILE=`find . -name aprs-framework\*.jar | head -n 1`;

if test -f pom.xml ; then 
    if test ! -f "${JARFILE}" ; then
        mvn -version || ( echo "Please install maven." && false)
        mvn -Pskip_tests package
    fi;
fi;


\rm -f run[0-9]*.log run[0-9]*.err >/dev/null 2>/dev/null || true
export JARFILE=`find . -name aprs-framework\*.jar | head -n 1`;

java -jar "${JARFILE}" $* > run$$.log 2> run$$.err

