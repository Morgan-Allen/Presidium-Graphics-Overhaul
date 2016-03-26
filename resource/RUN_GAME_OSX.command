#!/bin/bash
cd "$(dirname "$BASH_SOURCE")"

java -cp commons-math3-3.2.jar:gdx-backend-lwjgl-natives.jar:gdx-backend-lwjgl.jar:gdx-natives.jar:gdx.jar:stratos.jar stratos.start.DebugStartup