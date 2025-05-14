@ECHO OFF
if Defined JAVA_HOME (
	ECHO "JAVA_HOME %JAVA_HOME%"
) else (
	ECHO "JAVA_HOME %JAVA_HOME%"
	SET JAVA_HOME=C:\inst\jdk-17.0.7
	ECHO "JAVA_HOME %JAVA_HOME%"
)

call gradlew clean build run
REM call gradlew run
java -jar qry.jar
