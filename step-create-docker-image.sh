#!/bin/bash

set -e

RUN_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $RUN_PATH

echo ----[ Prepare folder for docker image ]----
DOCKER_BUILD=$RUN_PATH/build/docker

rm -rf $DOCKER_BUILD
mkdir -p $DOCKER_BUILD/app

cp -v build/libs/foilen-clouds-manager-$VERSION-boot.jar $DOCKER_BUILD/app/foilen-clouds-manager.jar
cp -v docker-release/* $DOCKER_BUILD

echo ----[ Docker image folder content ]----
find $DOCKER_BUILD

echo ----[ Build docker image ]----
DOCKER_IMAGE=foilen-clouds-manager:$VERSION
docker build -t $DOCKER_IMAGE $DOCKER_BUILD
docker tag $DOCKER_IMAGE foilen/$DOCKER_IMAGE

rm -rf $DOCKER_BUILD
