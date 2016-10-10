#!/bin/bash

GO=`which go`
PATH_TO_SCRIPT=`readlink -f ${0}`
PATH_TO_FOLDER=`dirname "$PATH_TO_SCRIPT"`

if [ "x$GO" == "x" ]; then
    echo "go missing, please install go 1.x"
    exit 1
fi

GOPATH=`cd "${PATH_TO_FOLDER}/.." && pwd -P`
export "GOPATH=${GOPATH}"
echo "fetiching sling ..."
go get github.com/dghubble/sling
echo "building orchent ..."
go build ${GOPATH}/src/orchent/orchent.go
echo "done"
