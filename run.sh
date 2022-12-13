#!/bin/sh
dirs="breakthrough experiments framework mcts"
#class=`shift`
class="breakthrough.Game"
CP="."
for d in $dirs
do
  CP="$CP:build/$d"
done

java -Xmx2048m -Xms512m -XX:+UseSerialGC -cp /home/mago/Desktop/breakthrough/lib/commons-math3-3.6.1.jar:/home/mago/Desktop/breakthrough/lib/xchart-3.6.0.jar:build $class $@
#java -Xmx2048m -Xms512m -XX:+UseSerialGC  $(find build -name Game.class) $@

# Classes with main 
#
#   amazons.gui.Amazons
#   cannon.gui.CannonGui
#   chinesecheckers.gui.CCGui
#   pentalath.gui.PentalathGui
#   lostcities.Game
#   experiments.AITests
# 


