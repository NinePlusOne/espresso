#!/bin/bash
# Linux/Mac shell script to run Espresso Chat

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Set the classpath to include the main JAR and all dependencies
CLASSPATH="$SCRIPT_DIR/espresso-chat-1.0-SNAPSHOT.jar:$SCRIPT_DIR/lib/*"

# Run the application with JavaFX module path
java --module-path "$SCRIPT_DIR/lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web -cp "$CLASSPATH" com.espresso.client.ClientMain