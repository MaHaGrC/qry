#!/usr/bin/bash
set -x
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd ${SCRIPT_DIR}\..
firefox http://localhost:8080/ &
java -jar qry.jar

