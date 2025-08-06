# JavaFX Deployment Guide for Espresso Chat

## Problem Solved

The original issue was that the JavaFX application failed to run when packaged as a JAR file with the error:
```
Error initializing QuantumRenderer: no suitable pipeline found
java.lang.RuntimeException: No toolkit found
```

This is a common problem with JavaFX applications since Java 11, where JavaFX was removed from the JDK and became a separate module system.

## Solution Implemented

### 1. Updated Maven Configuration

**Added platform-specific JavaFX dependencies:**
- Windows (`-win` classifier)
- Linux (`-linux` classifier) 
- macOS (`-mac` classifier)

This ensures all platform-specific native libraries are included in the distribution.

**Replaced Maven Shade Plugin with a better approach:**
- Maven Dependency Plugin: Copies all dependencies to `lib/` folder
- Maven JAR Plugin: Creates main JAR with proper classpath references
- Maven Assembly Plugin: Creates a complete distribution package

### 2. Created Distribution Package

The new build process creates:
- `espresso-chat-1.0-SNAPSHOT-distribution.zip` - Complete distribution package
- Contains main JAR, all dependencies, and run scripts
- Cross-platform compatible

### 3. Added Run Scripts

**Windows (`run.bat`):**
```batch
java --module-path "lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web -cp "espresso-chat-1.0-SNAPSHOT.jar;lib/*" com.espresso.client.ClientMain
```

**Linux/Mac (`run.sh`):**
```bash
java --module-path "lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web -cp "espresso-chat-1.0-SNAPSHOT.jar:lib/*" com.espresso.client.ClientMain
```

## How to Build and Deploy

### Building the Application

```bash
mvn clean package
```

This creates:
- `target/espresso-chat-1.0-SNAPSHOT-distribution.zip` - Distribution package
- `target/espresso-chat-1.0-SNAPSHOT.jar` - Main application JAR
- `target/lib/` - All dependencies including JavaFX

### Deploying to Users

1. **Share the distribution ZIP file** (`espresso-chat-1.0-SNAPSHOT-distribution.zip`)
2. **User extracts the ZIP** to any folder
3. **User runs the appropriate script:**
   - Windows: Double-click `run.bat` or run from command line
   - Linux/Mac: Run `./run.sh` from terminal (may need `chmod +x run.sh`)

### Requirements for End Users

- **Java 17 or higher** installed and in PATH
- **No additional JavaFX installation required** (included in distribution)
- **All platforms supported** (Windows, Linux, macOS)

## Why This Solution Works

1. **Platform-specific natives:** Includes JavaFX native libraries for all platforms
2. **Proper module path:** Uses `--module-path` to specify JavaFX location
3. **Module declarations:** Explicitly adds required JavaFX modules
4. **Correct classpath:** Separates application JAR from dependencies
5. **No fat JAR issues:** Avoids module system conflicts that occur with shaded JARs

## Alternative Manual Execution

If the run scripts don't work, users can run manually:

**Windows:**
```cmd
java --module-path "lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web -cp "espresso-chat-1.0-SNAPSHOT.jar;lib/*" com.espresso.client.ClientMain
```

**Linux/Mac:**
```bash
java --module-path "lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web -cp "espresso-chat-1.0-SNAPSHOT.jar:lib/*" com.espresso.client.ClientMain
```

## Troubleshooting

### Common Issues:

1. **"java command not found"**
   - Solution: Install Java 17+ and ensure it's in PATH

2. **"Module not found" errors**
   - Solution: Ensure all files from the ZIP are in the same directory

3. **Permission denied (Linux/Mac)**
   - Solution: Run `chmod +x run.sh` before executing

4. **Still getting JavaFX errors**
   - Solution: Verify Java version is 17+ with `java -version`
   - Ensure using the provided run scripts, not `java -jar`

### Verification:

To verify the distribution works, extract the ZIP and run:
```bash
# Check Java version
java -version

# List contents
ls -la

# Run application (Linux/Mac)
./run.sh

# Or run application (Windows)
run.bat
```

## Benefits of This Approach

1. **Cross-platform compatibility** - Works on Windows, Linux, and macOS
2. **Self-contained** - No need for users to install JavaFX separately
3. **Easy distribution** - Single ZIP file contains everything
4. **Maintainable** - Clear separation of concerns in build process
5. **Future-proof** - Compatible with modern Java module system