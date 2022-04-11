#!/bin/bash

###############################################################
# Free deployment resources when finished with the code example
###############################################################

USE_NGROK=false
#EXAMPLE_NAME='haapi'
EXAMPLE_NAME='haapi-android-fallback'
./deployment/stop.sh "$USE_NGROK" "$EXAMPLE_NAME"
