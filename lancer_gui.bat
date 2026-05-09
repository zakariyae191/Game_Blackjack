@echo off
if not exist "%~dp0out\blackjack\BlackjackGUIClient.class" call "%~dp0build.bat"
echo Lancement du client GRAPHIQUE Blackjack...
java -cp "%~dp0out" blackjack.BlackjackGUIClient localhost
pause
