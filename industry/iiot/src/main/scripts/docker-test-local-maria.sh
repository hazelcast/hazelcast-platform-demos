#!/bin/bash

DOCKER_PORT_INTERNAL=3306
DOCKER_PORT_EXTERNAL=${DOCKER_PORT_INTERNAL}

. `dirname $0`/env-docker.sh
