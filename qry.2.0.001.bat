cd %0\..
git status
call gradlew build
start firefox http://localhost:8081/
java -jar qry.jar
PAUSE