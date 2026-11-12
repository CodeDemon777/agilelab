@echo off
where mvn >nul 2>nul
if not errorlevel 1 (
    mvn %*
) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\plugins\maven\lib\maven3\bin\mvn.cmd" (
    "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\plugins\maven\lib\maven3\bin\mvn.cmd" %*
) else (
    echo Error: Maven executable not found.
    exit /b 1
)
