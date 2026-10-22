@echo off
setlocal
rem Run the auth-service Maven Wrapper from the repository root.
pushd "%~dp0services\auth-service" || exit /b 1
call .\mvnw.cmd %*
set "BUILD_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %BUILD_EXIT_CODE%
