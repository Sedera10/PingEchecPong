@echo off
REM Déploiement de compte-courant.war sur WildFly

REM Chemin vers le fichier WAR
set WAR_SOURCE=D:\ITU\S5\RaTah\PingEchecPong\backend\ejb\target\chess-ejb.war

REM Chemin vers le dossier deployments de WildFly
set DEPLOY_DIR=C:\Program Files\wildfly-37.0.1.Final\standalone\deployments

REM Copier le WAR dans le dossier deployments
echo Copie du WAR vers WildFly...
copy "%WAR_SOURCE%" "%DEPLOY_DIR%"

if %errorlevel% == 0 (
    echo Déploiement réussi !
) else (
    echo Erreur lors de la copie.
)

pause
