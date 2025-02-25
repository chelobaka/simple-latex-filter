# Simple LaTeX filter plugin for OmegaT

## What is it?

This plugin implements a subset of LaTeX syntax as OmegaT file filter. Translated content is
expected to be Unicode encoded.

Since LaTeX commands are numerous and documents can contain custom commands, it is a good idea
to describe all unknown commands before starting the translation process. See the [manual](CONFIGURATION.md)
for details.

This filter doesn't cover all cases of LaTeX control sequences. If you stumble upon something you
can't fix with manual configuration, then create an issue.


## Building

The filter uses Jackson library for reading JSON, while OmegaT started to use Jackson
between versions 5.7 and 5.8. You can build the plugin with or without Jackson included depending
on OmegaT version you are going to use. Fat version should work with any OmegaT version.

Slim version without Jackson can be built with command:
```
./gradlew jar 
```
Fat version with Jackson bundled is created this way:
```
./gradlew fatJar
```
After build is complete plugin files can be found in `build/libs` directory in the source tree. 

## Install

Copy plugin JAR file into OmegaT user plugin directory: 
* Windows XP: `Documents and Settings\<UserName>\Application Data\OmegaT\plugins`
* Windows Vista and later: `User\<UserName>\AppData\Roaming\OmegaT\plugins`
* Mac OS X: `~/Library/Preferences/OmegaT/plugins`
* Linux, FreeBSD, Solaris: `~/.omegat/plugins`


## Configuration
See [here](CONFIGURATION.md).

## License

This project is distributed under the GNU general public license version 3 or later.

