@echo off
where java >nul 2>nul || (echo ERROR: Install JDK 8.& exit /b 1)
java -version 2>&1 | findstr /C:"1.8." >nul || (echo ERROR: This project requires JDK 8.& exit /b 1)
gradle --version 2>&1 | findstr /C:"Gradle 4.10.3" >nul || (echo ERROR: Install Gradle 4.10.3 and add it to PATH.& exit /b 1)
gradle setupDecompWorkspace build
