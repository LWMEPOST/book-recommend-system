@REM Lightweight Maven bootstrap script for local development.
@echo off
setlocal

if not defined JAVA_HOME (
    if exist "C:\Users\LWM\.jdks\oracle_open_jdk-17\bin\java.exe" (
        set "JAVA_HOME=C:\Users\LWM\.jdks\oracle_open_jdk-17"
    )
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

set "MAVEN_CMD_LINE_ARGS=%*"
set "MAVEN_VERSION=3.9.11"
set "TEMP_MAVEN_HOME=%TEMP%\apache-maven-%MAVEN_VERSION%"
set "TEMP_MVN_CMD=%TEMP_MAVEN_HOME%\bin\mvn.cmd"

where mvn >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set "MVN_CMD=mvn"
    goto runMaven
)

if exist "%TEMP_MVN_CMD%" (
    set "MVN_CMD=%TEMP_MVN_CMD%"
    goto runMaven
)

echo Maven not found in PATH. Bootstrapping Apache Maven %MAVEN_VERSION% into %TEMP%...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$zipPath = Join-Path $env:TEMP 'apache-maven-%MAVEN_VERSION%-bin.zip';" ^
    "$mavenHome = Join-Path $env:TEMP 'apache-maven-%MAVEN_VERSION%';" ^
    "if (-not (Test-Path $zipPath)) { Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $zipPath };" ^
    "if (-not (Test-Path (Join-Path $mavenHome 'bin\mvn.cmd'))) { Expand-Archive -Path $zipPath -DestinationPath $env:TEMP -Force }"

if errorlevel 1 (
    echo Failed to bootstrap Maven automatically.
    exit /b 1
)

if not exist "%TEMP_MVN_CMD%" (
    echo Maven bootstrap completed but mvn.cmd was not found.
    exit /b 1
)

set "MVN_CMD=%TEMP_MVN_CMD%"

:runMaven
call "%MVN_CMD%" %MAVEN_CMD_LINE_ARGS%
exit /b %ERRORLEVEL%
