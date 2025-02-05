#!/usr/bin/env bash
# Arguments:
# 1: next version
# 2: channel

source "$(dirname "$0")/docker-common.sh" $1 $2

# Push docker images (built previously)
docker buildx build \
    --platform $PLATFORMS \
    --tag wabayang/jelu:$DOCKER_CHANNEL \
    --tag wabayang/jelu:$1 \
    --file ./Dockerfile . \
    --push
