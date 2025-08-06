Espresso Chat Application
========================

This distribution contains the Espresso Chat application with all required dependencies.

Requirements:
- Java 17 or higher
- No additional JavaFX installation required (included in this distribution)

Running the Application:

Windows:
--------
Double-click on "run.bat" or open Command Prompt/PowerShell and run:
    run.bat

Linux/Mac:
----------
Open terminal and run:
    chmod +x run.sh
    ./run.sh

Alternative Manual Execution:
-----------------------------
If the scripts don't work, you can run manually:

Windows:
java --module-path "lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web -cp "espresso-chat-1.0-SNAPSHOT.jar;lib/*" com.espresso.client.ClientMain

Linux/Mac:
java --module-path "lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web -cp "espresso-chat-1.0-SNAPSHOT.jar:lib/*" com.espresso.client.ClientMain

Troubleshooting:
---------------
- Make sure Java 17+ is installed and in your PATH
- Ensure all files from this distribution are in the same directory
- On Linux/Mac, make sure run.sh has execute permissions (chmod +x run.sh)