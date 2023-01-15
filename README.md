# clocunchecked
Count lines of code that are annotated with @NullUnmarked in source code.

#### Running

First compile and create the jar file using:
```shell
./gradlew publishToMavenLocal

```

Then, run the created jar with the path of root directory where all source files exists.
```shell
java -jar ClocUnchecked.jar --path path-to-root-src
```
