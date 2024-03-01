# building Jelu locally

## prerequisites

- java 17
- Jelu code source

## building

just `cd` in the root of the repo and run :

```shell
./gradlew copyWebDist && ./gradlew assemble
```

Gradlew is included in the sources so you don't need anything else.

Those commands will build the frontend and then the backend including the fat jar.

You will find the jar in `build/libs`, it will be named `jelu-<current-version>.jar` 

The jar is the same as the one that is published in github releases.

