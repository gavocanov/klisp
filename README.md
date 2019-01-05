# klisp

A lisp-alike interpreter written in Kotlin multiplatform, don't ask....it's just fun ;)

Done starting from [here](http://norvig.com/lispy.html), and then it just got too fun to stop.

Native is tested/setup only for MacOS using [linenoise](https://github.com/antirez/linenoise) for light readline support.
Jvm version is using [JLine](https://github.com/jline/jline3) for readline.

Source code for console colors is included (because I'm too lazy for a proper multiplatform PR :/)
from the excellent [mordant](https://github.com/ajalt/mordant) and [colormath](https://github.com/ajalt/colormath) libs,
originally only for JVM.

## build

* ```make dirmake``` - to prepare native target dirs
* ```make``` - to compile native target linenoise lib
* ```./gradlew check``` - for tests (both native/jvm)
* ```./gradlew build``` - to build both jvm and native executables

* ```./gradlew tasks``` - will show all the available tasks if you need anything else.

## run

* jvm - ```kotlin build/libs/klisp-0.0.1.jar```
* native release - ```build/bin/nat/mainReleaseExecutable/main.kexe```
* native debug - ```build/bin/nat/mainDebugExecutable/main.kexe```

## debug/develop
I'm using Intellij IDEA for the jvm target and Intellij CLion for the native target, for both debug/run works really 
smooth, only prob is native compile/link times are pretty slow atm, guess JetBrains will fix that soon.


