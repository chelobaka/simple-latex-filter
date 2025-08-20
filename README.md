# Simple LaTeX filter plugin for OmegaT

## What is it?
This plugin implements a subset of LaTeX syntax as OmegaT file filter. Translated content is
expected to be Unicode encoded.

Since LaTeX commands are numerous and documents can contain custom commands, it is a good idea
to describe all unknown commands before starting the translation process. See the [manual](CONFIGURATION.md)
for details.

This filter doesn't cover all cases of LaTeX control sequences. If you stumble upon something you
can't fix with manual configuration, then create an issue.

## Requirements
Omegat 6.0 or later.

## Building
Inside source code folder run:
```
./gradlew jar 
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

