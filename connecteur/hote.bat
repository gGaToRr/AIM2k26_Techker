@echo off
rem Chrome ne sait pas lancer un .ps1 directement : ce .bat sert de relais.
powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0hote.ps1"
