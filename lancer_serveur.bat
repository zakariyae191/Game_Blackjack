@echo off
call "%~dp0build.bat"
if %errorlevel% neq 0 (
    pause
    exit /b %errorlevel%
)
echo Lancement du serveur...
java -cp "%~dp0out" blackjack.BlackjackServer
pause
