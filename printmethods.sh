#!/bin/sh

JAVAFILE=$1
CLASSPATH=.:asm-6.1.1.jar
if [ "$#" -ne 1 ]; then
  echo "Wrong number of arguments"
  exit 1
fi
if [ ! -e $JAVAFILE.java ]; then
  echo "${JAVAFILE}.java File does not exist"
  exit 1
fi
javac -cp $CLASSPATH MethodCallLog.java
javac $JAVAFILE.java
java -cp $CLASSPATH MethodCallLog $JAVAFILE.class $JAVAFILE.class
java $JAVAFILE
