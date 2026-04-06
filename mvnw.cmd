@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __ MVNW_CMD__=%COMSPEC%
@IF a%COMSPEC%==a SET __MVNW_CMD__=%SystemRoot%\system32\cmd.exe

@setlocal
@SET JAVA_HOME=%JAVA_HOME%
@SET MAVEN_PROJECTBASEDIR=%~dp0

@SET MVNW_REPOURL=https://repo.maven.apache.org/maven2
@SET MVNW_DISTRIBUTION_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip

@SET MAVEN_USER_HOME=%USERPROFILE%\.m2
@SET MVNW_FOLDER=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-3.9.9

@IF NOT EXIST "%MVNW_FOLDER%\apache-maven-3.9.9\bin\mvn.cmd" (
  @ECHO Downloading Maven 3.9.9...
  @IF NOT EXIST "%MAVEN_USER_HOME%\wrapper\dists" MKDIR "%MAVEN_USER_HOME%\wrapper\dists"
  @powershell -Command "Invoke-WebRequest -Uri '%MVNW_DISTRIBUTION_URL%' -OutFile '%MAVEN_USER_HOME%\wrapper\apache-maven-3.9.9-bin.zip'"
  @powershell -Command "Expand-Archive -Path '%MAVEN_USER_HOME%\wrapper\apache-maven-3.9.9-bin.zip' -DestinationPath '%MVNW_FOLDER%' -Force"
)

@SET MVNW_EXE=%MVNW_FOLDER%\apache-maven-3.9.9\bin\mvn.cmd
@"%MVNW_EXE%" %*
