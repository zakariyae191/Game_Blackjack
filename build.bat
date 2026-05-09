@echo off
setlocal

set "ROOT=%~dp0"
set "SRC=%ROOT%src"
set "OUT=%ROOT%out"

echo Compilation du projet Blackjack...
if not exist "%OUT%" mkdir "%OUT%"

javac -encoding UTF-8 -d "%OUT%" ^
    "%SRC%\blackjack\Card.java" ^
    "%SRC%\blackjack\Deck.java" ^
    "%SRC%\blackjack\BlackjackServer.java" ^
    "%SRC%\blackjack\BlackjackClient.java" ^
    "%SRC%\blackjack\BlackjackGUIClient.java"
if %errorlevel% neq 0 (
    echo Erreur de compilation.
    exit /b %errorlevel%
)

echo Compilation terminee: %OUT%
