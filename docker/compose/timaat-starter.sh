#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

createSpeechToTextServiceCertificates() {
  local certDir="${1:-$SCRIPT_DIR/certs}"
  local certFile="$certDir/speechToTextService.crt"
  local keyFile="$certDir/speechToTextService.key"

  if [[ -f "$certFile" && -f "$keyFile" ]]; then
    return 0
  fi

  openssl version >/dev/null 2>&1 || { echo >&2 "openssl is required to create the self-signed certificate for the speech-to-text service."; return 1; }

  echo "Creating self-signed certificate for the speech-to-text service"
  mkdir -p "$certDir" || return 1
  openssl req -x509 -newkey rsa:4096 -sha256 -days 73000 -nodes \
    -keyout "$keyFile" -out "$certFile" \
    -subj "/CN=nko-dev.studio" \
    -addext "subjectAltName=DNS:speechToTextService"
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  echo Starting TIMAAT
  docker -v  >/dev/null 2>&1 || { echo >&2 "A docker runtime is required to start TIMAAT."; exit 1; }

  createSpeechToTextServiceCertificates "$SCRIPT_DIR/certs" || exit 1

  docker compose up
fi