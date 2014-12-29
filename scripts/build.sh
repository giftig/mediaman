#!/bin/bash

if [[ "$1" == "" ]]; then
  echo 'You need to specify a production config file to build into the package'
  exit 1
fi
CONFIG_FILE="$1"

if [[ ! $(ls "$CONFIG_FILE") ]]; then
  echo 'The config file does not exist. Check the path?'
  exit 1
fi

cp "$CONFIG_FILE" src/main/resources/application.conf

mvn test

if [[ "$?" != 0 ]];
  echo 'Tests failed; aborting build.'
  exit 2
fi

mvn clean package

if [[ "$?" != 0 ]];
  echo 'Package step failed!'
  exit 3
fi

echo 'Looks like everything went fine.'
echo "The following files were built:"

ls build/mediaman*.jar

# Don't need this anymore
rm src/main/resources/application.conf
