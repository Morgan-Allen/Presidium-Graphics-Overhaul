#!/bin/bash
cd "$(dirname "$BASH_SOURCE")"

cd ../../..
./Stratos.app/Contents/Plugins/OSX_JRE/Contents/Home/jre/bin/java -Xdock:icon=./Stratos.app/Contents/Resources/game_icon.icns -cp ./Stratos.app/Contents/Java/stratos.jar stratos.start.DebugStartup