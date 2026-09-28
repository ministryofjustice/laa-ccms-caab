#!/usr/bin/env bash
# Checks each link from the apps to SOA dev: tunnel, SOA, proxy.

set -uo pipefail

TUNNEL_PORT=8052
timeout_run() { perl -e 'alarm shift; exec @ARGV' "$@"; }
hr() { printf '\n%s\n' "------------------------------------------------------------"; }
fail=0
tunnel_up=0

hr; echo "1. SSM port-forward listening on 127.0.0.1:$TUNNEL_PORT?"
if lsof -nP -iTCP:"$TUNNEL_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  lsof -nP -iTCP:"$TUNNEL_PORT" -sTCP:LISTEN 2>/dev/null | tail -n +2 | awk '{print "   " $1 " (pid " $2 ")"}'
  tunnel_up=1
else
  echo "   NO. Start it with localPortNumber $TUNNEL_PORT."; fail=1
fi

hr; echo "2. SOA answers over TLS through the tunnel?"
if [ "$tunnel_up" -eq 1 ]; then
  CODE=$(timeout_run 20 curl -sk -o /dev/null -w '%{http_code}' --max-time 15 "https://127.0.0.1:$TUNNEL_PORT/soa-infra/" 2>/dev/null)
  case "${CODE:-000}" in
    000) echo "   NO response. A stale SSM session is the usual cause: restart it and retry."; fail=1 ;;
    *)   echo "   yes: HTTP $CODE" ;;
  esac
else
  echo "   skipped, no tunnel"
fi

hr; echo "3. Proxy up and passing calls through on localhost:8051?"
if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx laa-ccms-soa-proxy; then
  if [ "$tunnel_up" -eq 1 ]; then
    CODE=$(timeout_run 20 curl -s -o /dev/null -w '%{http_code}' --max-time 15 "http://localhost:8051/soa-infra/" 2>/dev/null)
    case "${CODE:-000}" in
      000)     echo "   container running but no response; see: docker logs laa-ccms-soa-proxy"; fail=1 ;;
      502|504) echo "   HTTP $CODE from nginx: it could not reach SOA; see: docker logs laa-ccms-soa-proxy"; fail=1 ;;
      *)       echo "   yes: HTTP $CODE (SOA, via the proxy)" ;;
    esac
  else
    echo "   container running; pass-through not checked without a tunnel"
  fi
else
  echo "   NO. Start it: docker-compose --compatibility -p laa-ccms-caab-development up -d laa-ccms-soa-proxy"; fail=1
fi

hr
[ "$fail" -eq 0 ] && echo "All good: the apps can use http://localhost:8051 as before." || echo "Something needs fixing, see above."
exit "$fail"
