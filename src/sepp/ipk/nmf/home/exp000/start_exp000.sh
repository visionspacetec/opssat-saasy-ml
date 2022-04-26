#!/bin/sh

# NMF_LIB can be provided by the parent app (i.e. supervisor) or set locally
if [ -z "$NMF_LIB" ] ; then
    NMF_LIB=`cd ../nmf/lib > /dev/null; pwd`
fi

if [ -z "$NMF_HOME" ] ; then
    NMF_HOME=`cd ../nmf > /dev/null; pwd`
fi

if [ -z "$MAX_HEAP" ] ; then
    MAX_HEAP=128m
fi

JAVA_OPTS="-Xms32m -Xmx$MAX_HEAP $JAVA_OPTS"
LOCAL_LIB_PATH=`readlink -f lib`
LD_LIBRARY_PATH=$LOCAL_LIB_PATH:`cd ../nmf/lib > /dev/null; pwd`:$LD_LIBRARY_PATH

export JAVA_OPTS
export NMF_LIB
export NMF_HOME
export LD_LIBRARY_PATH

# Replaced with the main class name
MAIN_CLASS_NAME=esa.mo.nmf.apps.PayloadsTestApp

exec java $JAVA_OPTS \
  -classpath "$NMF_LIB/*:lib/*:/usr/lib/java/*" \
  -Dnmf.platform.impl=@PLATFORM@ \
  -Djava.util.logging.config.file="$NMF_HOME/logging.properties" \
  "$MAIN_CLASS_NAME" \
  "$@"

