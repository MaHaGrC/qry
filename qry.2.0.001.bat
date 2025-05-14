cd %0\..
git status
call gradlew build
start firefox http://localhost:8080/
java -jar qry.jar
PAUSE