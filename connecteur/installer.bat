@echo off
rem Double-cliquable : lance installer.ps1 sans changer la politique d'execution du systeme.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0installer.ps1" %*
pause
