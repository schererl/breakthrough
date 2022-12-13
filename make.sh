#!/bin/sh
dirs="breakthrough experiments framework mcts"

for d in $dirs
do
  	echo "hi"
	mkdir -p build/$d
	cp -r src/$d/* build/$d
done

files=`find build -name *.java`
javac -cp /home/mago/Desktop/breakthrough/lib/commons-math3-3.6.1.jar:/home/mago/Desktop/breakthrough/lib/xchart-3.6.0.jar -d build $files

