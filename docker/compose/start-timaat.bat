@echo off
echo Starting TIMAAT...

where docker >nul 2>&1
if errorlevel 1 (
    echo A Docker runtime is required to start TIMAAT.
    exit /b 1
)

call :createSpeechToTextServiceCertificates "%~dp0certs"
if errorlevel 1 exit /b 1

docker compose up
exit /b %errorlevel%

:createSpeechToTextServiceCertificates
if exist "%~1\speechToTextService.crt" if exist "%~1\speechToTextService.key" exit /b 0

where openssl >nul 2>&1
if errorlevel 1 (
    echo openssl is required to create the self-signed certificate for the speech-to-text service.
    exit /b 1
)

echo Creating self-signed certificate for the speech-to-text service
if not exist "%~1" (
    mkdir "%~1"
    if errorlevel 1 exit /b 1
)
openssl req -x509 -newkey rsa:4096 -sha256 -days 73000 -nodes ^
    -keyout "%~1\speechToTextService.key" -out "%~1\speechToTextService.crt" ^
    -subj "/CN=nko-dev.studio" ^
    -addext "subjectAltName=DNS:speechToTextService"
exit /b %errorlevel%