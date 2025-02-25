#!/usr/bin/python3

import argparse
import json
import re
from collections import defaultdict
from pathlib import Path


def validate_config(path: Path) -> set[str]:
    tag_re_string = r'^([a-zA-Z]+)(\d*)$'
    tag_pattern = re.compile(tag_re_string)

    found_commands = set()
    alpha_tags = set()
    numbered_tags = defaultdict(set)

    try:
        config = json.load(path.open())
    except Exception as e:
        print("Error loading configuration file")
        print(e)
        exit()

    try:
        for command_block in config['allCommands']:
            command_type = command_block["type"]
            for command in command_block["commands"]:

                if type(command) is str:
                    command_name = command
                elif type(command) is dict:
                    command_name = command['name']
                else:
                    print(f"ERROR: Command definition should be a string or an object. Found {type(command)}.")
                    exit()

                if command_name in found_commands:
                    print(f'WARNING: Command <{command_name}> has a duplicate entry.')
                    continue  # Do not check duplicate
                else:
                    found_commands.add(command_name)

                # Check tags
                if command_type != 'FORMAT':
                    continue
                if 'tag' not in command:
                    print(f'ERROR: Command <{command_name}> has FORMAT type and must have a <tag> property.')
                    exit()
                tag: str = command['tag']

                match = tag_pattern.match(tag)
                if match is None:
                    print(f"ERROR: Tag <{tag}> doesn't match tag name pattern {tag_re_string}")
                    exit()

                tag_letters, tag_number = match.groups()
                if len(tag_number) == 0:
                    tag_number = None
                else:
                    tag_number = int(tag_number)

                # Tag name without digits
                if tag_number is None:
                    # Alpha tags should have an external argument for auto-numbering
                    if 'args' in command:
                        command_args = command['args']
                    else:
                        command_args = []  # Not really but OK for this check
                    if command_args is None:
                        command_args = []
                    has_external_argument = False
                    for arg in command_args:
                        if 'external' in arg and arg['external'] is True:
                            has_external_argument = True
                            break
                    if not has_external_argument:
                        print(f'WARNING: Command <{command_name}> has unnumbered tag <{tag}> which is reserved ' +
                              'for commands with an external argument.')

                    if tag in alpha_tags:
                        print(f"ERROR: Tag <{tag}> has a duplicate entry.")
                        exit()

                    if len(numbered_tags[tag]) > 0 and max(numbered_tags[tag]) > 0:
                        print(f"ERROR: Tag name <{tag}> conflicts with other numbered tag(s).")
                        exit()

                    alpha_tags.add(tag)

                else:  # Numbered tag
                    if tag_number in numbered_tags[tag_letters]:
                        print(f"ERROR: Tag <{tag}> has a duplicate entry.")
                        exit()

                    if tag_number > 0 and tag_letters in alpha_tags:
                        print(f"ERROR: Tag <{tag}> conflicts with unnumbered tag <{tag_letters}>.")
                        exit()

                    numbered_tags[tag_letters].add(tag_number)

    except Exception as e:
        print("Configuration file is damaged.")
        print(e)
        exit()

    print('Configuration file integrity verified.')
    return found_commands


def check_against_content(ext_commands: set[str], content_path: Path):

    env_commands = ('begin', 'end')
    command_pattern = re.compile(r'(?<!\\)\\([a-z]{2,})')
    unknown_commands = set()

    print('Checking commands in provided document(s)...')

    if not content_path.exists():
        print("ERROR: Content path doesn't exist.")
        exit()

    if content_path.is_dir():
        doc_paths = content_path.glob('*.tex')
    else:
        doc_paths = [content_path]

    for path in doc_paths:
        doc = path.open().read()
        for mc in command_pattern.findall(doc):
            if mc in unknown_commands:
                continue
            if mc not in ext_commands and mc not in env_commands:
                unknown_commands.add(mc)

    if len(unknown_commands) > 0:
        print('Found unknown commands in provided document(s):')
        for uc in sorted(unknown_commands):
            print(uc)
    else:
        print("Provided content doesn't contain any unknown commands.")


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("config_path", help="path to JSON configuration file")
    parser.add_argument("content_path", nargs='?', help="path to LaTeX content (dir or file) to check against",
                        default=None)
    args = parser.parse_args()
    config_path = Path(args.config_path)
    if not config_path.exists() or not config_path.is_file():
        print("Invalid configuration file path supplied.")
        exit()

    known_commands = validate_config(config_path)

    if args.content_path is None:
        exit()

    check_against_content(known_commands, Path(args.content_path))
