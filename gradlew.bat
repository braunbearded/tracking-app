@echo off
setlocal

set APP_HOME=%~dp0
if not defined GRADLE_USER_HOME set GRADLE_USER_HOME=%APP_HOME%\.gradle
set MAIN_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
set SHARED_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper-shared.jar
set GRADLE_LIBS=/usr/share/java/gradle/lib/*

if defined JAVA_HOME (
  set JAVA_CMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_CMD=java
)

"%JAVA_CMD%" -classpath "%MAIN_JAR%;%SHARED_JAR%;%GRADLE_LIBS%" org.gradle.wrapper.GradleWrapperMain %*
