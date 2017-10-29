gradle-fidata-plugin
====================

`org.fidata.project`:
1. Applies `nebula.contacts` plugin
2. Applies artifactory plugin
3. Applies `nebula.dependency-lock` plugin
4. Applies `com.github.ben-manes.versions` plugin
5. Applies `semantic-release` plugin
6. Sets `group` to `org.fidata`
7. Provides ext.isRelease
8. Provides ext.changeLog
9. Configures wrapper task
10. Configures report dir
11. Provides `codenarcBuildSrc` task
12. Includes all CodeNarc tasks in execution list for check task
13. Includes all Test tasks in execution list for check task
14. Sets build name

Requires JDK >= 8
and Gradle >= 3.0