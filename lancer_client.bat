@echo off
if not exist "%~dp0out\blackjack\BlackjackClient.class" call "%~dp0build.bat"
echo Lancement du client Blackjack...
java -cp "%~dp0out" blackjack.BlackjackClient localhost
pause
