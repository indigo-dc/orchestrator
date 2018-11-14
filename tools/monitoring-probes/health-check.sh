#!/bin/bash

print_help (){
echo "
   usage: $1 [-h] -u URL [-t TIMEOUT] [-v]

   -h            print this help
   -u URL        mandatory URL of the PaaS Orchestrator (e.g. https://indigo-paas.cloud.ba.infn.it/orchestrator)
   -t TIMEOUT    optional timeout after which the probe will be terminated
   -v            turn-on verbose mode
"
}

function check () {
  local url=$1
  if [[ $verbose ]]; then extraopts="-S"; fi
  results=$(curl \
    --max-time $timeout \
    --write-out ";%{http_code};%{time_total}\n" \
    --silent $extraopts \
    "$url")
  ret=$?
  http_code=$( echo "$results" | awk -F ";" 'NF > 1 {print $2}')
  time_total=$( echo "$results" | awk -F ";" 'NF > 1 {print $3}')
  response=$( echo "$results" | awk -F ";" '{print $1}')

  if [[ $ret != 0 ]]; then
    if [[ $verbose != true ]]; then
       echo Aborting. Run with verbose option to see error messages
    fi
    exit $EXIT_CRITICAL
  fi
}

#### MAIN ####
EXIT_OK=0
EXIT_CRITICAL=2
EXIT_ABORT=255

while getopts 't:u:hv' OPT; do
  case $OPT in
    t)  timeout=$OPTARG;;
    u)  url=$OPTARG ;;
    v)  verbose=true;;
    h)  print_help $0; exit $EXIT_ABORT;;
    *)  print_help $0; exit $EXIT_ABORT;;
  esac
done

if [[ -z "$url" ]]; then
   echo -e "$0: no URL specified!\ntry '$0 -h' for more information"
   exit $EXIT_ABORT
fi

if [[ $verbose ]]; then
  echo "Running Orchestrator probe..."
fi

timeout=${timeout:-15}
info_url=$url/info

check $info_url

if [[ $verbose ]]; then
  echo "HTTP code: \"$http_code\""
  echo "Response Time (s): $time_total"
  echo -e "Orchestrator response:\n$response"
fi

if [[ $http_code == 200 ]]; then
  exit $EXIT_OK;
else
  exit $EXIT_CRITICAL;
fi
