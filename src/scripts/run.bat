@echo off
REM Windows batch script to run Espresso Chat

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0

REM Set the classpath to include the main JAR and all dependencies
set CLASSPATH=%SCRIPT_DIR%espresso-chat-1.0-SNAPSHOT.jar;%SCRIPT_DIR%lib\*

REM Run the application with JavaFX module path
java --module-path "%SCRIPT_DIR%lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web -cp "%CLASSPATH%" com.espresso.client.ClientMain

pause