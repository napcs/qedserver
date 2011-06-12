#!/bin/sh
cd `dirname $0`/..
java -Xmx2048M -Djetty.home=../../../ -DSTART=bin/start.config -jar ../../../start.jar
