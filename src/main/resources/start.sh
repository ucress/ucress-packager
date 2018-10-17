#!/bin/sh

RUN_DIR=$PWD

if [ -f $PWD/bin/bootstrap.jar ] ; then
  APP_HOME=$PWD
else
  cd ..
  if [ -f $PWD/bin/bootstrap.jar ] ; then
    APP_HOME=$PWD
  fi
fi

if [ "$APP_HOME" = "" ] ; then
  echo "Can not find bootstrap.jar"
  cd $RUN_DIR
else
  echo JAVA_HOME : "$JAVA_HOME"
  echo APP_HOME  : "$APP_HOME"
  nohup $JAVA_HOME/bin/java "-Dapp.home=$APP_HOME" -classpath "bin/bootstrap.jar" com.ucress.loader.Launcher ${className} &
  cd $RUN_DIR
fi
