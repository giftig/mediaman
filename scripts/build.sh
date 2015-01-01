#!/bin/bash

# Check that the provided path to an external resource actually exists
# If it doesn't, fail hard and loudly.
check_external_resource() {
  ls "$1" &> /dev/null

  if [[ $? != 0 ]]; then
    echo "The resource '$1' doesn't exist. Check the path?"
    exit 1
  fi
}

# Show the script usage and exit with status $1
show_usage() {
  echo -e '\nUsage: build.sh -c application.conf [-l logback.xml]\n'
  echo 'Options:'
  echo '-c, --config: Mandatory application.conf file to build into the package'
  echo '-l, --logback-config: Optional logback.xml to replace the default'
  echo '-h, --help: Show this message and exit'
  exit $1
}

# Ensure everything is cleaned up and exit with status $1
finish() {
  rm src/main/resources/application.conf &> /dev/null

  ls .logback.xml.tmp &> /dev/null
  if [[ $? == 0 ]]; then
    mv .logback.xml.tmp src/main/resources/logback.xml
  fi

  exit $1
}

# Parse command options to ensure configurations are respected etc.
while [[ $# > 0 ]]; do
  key="$1"
  shift

  case $key in
    -c|--config)
      CONFIG_FILE="$1"
      check_external_resource $1
      ;;
    -l|--logback-config)
      LOGBACK_CONFIG_FILE="$1"
      check_external_resource $1
      ;;
    -h|--help)
      show_usage 0
      ;;
    *)
      show_usage 1
      ;;
  esac
  shift
done

if [[ "$CONFIG_FILE" == "" ]]; then
  show_usage 1
fi

cp "$CONFIG_FILE" src/main/resources/application.conf

if [[ "$LOGBACK_CONFIG_FILE" != "" ]]; then
  cp src/main/resources/logback.xml .logback.xml.tmp
  cp "$LOGBACK_CONFIG_FILE" src/main/resources/logback.xml
fi

mvn clean package

if [[ "$?" != 0 ]]; then
  echo 'Build failed!'
  finish 3
fi

echo -e "\n\n\n"
echo 'Looks like everything went fine.'
echo "The following assemblies were generated:"
echo -e '\n'

ls build/*.jar

finish 0
