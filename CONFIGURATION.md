# Simple LaTeX filter configuration

## Basics
The filter relies on internal configuration by default. Enabling user configuration can be done in filter options dialog.
After it is enabled internal configuration is copied to `SimpleLatexFilter.json` in OmegaT user configuration directory.
This file is never overwritten by the filter.

If you update the plugin, move your user configuration file somewhere else and let the filter create a new one. After
that you can copy your custom definitions into new configuration.

The configuration contains two root sections: `allCommands` and `environments`.

### Commands are divided into three groups:
* `FORMAT` — reserved to inline formatting commands, which are converted into OmegaT tags.
* `CONTENT` — for commands having its own content context like `\chapter{...}`.
* `CONTROL` — excluded from translation process.

Any command missing in configuration is treated as `FORMAT` one. Whether an unknown command will appear in translation
depends on its context. Everything inside `CONTROL` command arguments is considered as non-translatable.

### Command properties:
* `name` — should be clear by the name :-)
* `tag` — OmegaT tag name base, only for `FORMAT` commands. It can be complimented with a tag counter number by the filter.
* `args` — list of command arguments in case the command has something to translate inside them. Commands without
arguments should have `null` value for this property.

### Command arguments might have own properties:
* `translate` — whether an argument content should be translated.
* `external` — says that an argument content should be translated in a separate segment, like footnote text. `FORMAT`
commands having an external argument receive a tag counter number to their tag name. 
* `escape` — setting this property to `false` disables character escaping back and forth for argument content. Examples
are URLs in hyperlinks and verbatim content.

*A note about tag numbering:*

`FORMAT` commands with an external argument receive tag numbers to their names. The numbering starts with 1, so it is
safe to have `f0` used for `\footnotemark` and `f` for `\footnote` which becomes `f1`, `f2` and so on.

### Reserved tag names

Some tag names are used by the filter for special cases:
* `G0` — orphan (not following a command) group boundaries. For example `\emph {like this}`.
* `Ux` — unknown commands, where `x` is a tag number.
* `Mx` — masked content inside external arguments. For example a line break inside footnote text.
* `Mathx` — inline math content.

### Environments
Each property contains list of environment names:

* `consumeOptions` — these might have options going right after begin without any translatable content. 
* `consumeArguments`— same for arguments right after begin.
* `table` — cell detection is activated inside these environments.

## Helper script

There is a Python script `slf_checker.py` coming with the plugin which can used to detect inconsistencies
(not all of them for sure) in the configuration file and optionally check LaTeX content for unknown commands.

Usage:
```
python3 slf_checker.py <path-to-configuration-file> [path-to-content-directory-or-file]
```