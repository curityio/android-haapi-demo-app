#!/bin/bash

###############################################################
# Free deployment resources when finished with the code example
###############################################################

USE_NGROK=false
EXAMPLE_NAME='haapi'
./deployment/stop.sh "$USE_NGROK" "$EXAMPLE_NAME"
