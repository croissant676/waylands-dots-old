#!/bin/bash

CONFIG_FILES="$HOME/.config/waybar/config.jsonc $HOME/.config/waybar/style.css"

trap "killall waybar" EXIT

while true; do
    waybar -l debug &
    # shellcheck disable=SC2086
    inotifywait -e create,modify $CONFIG_FILES
    killall waybar
done
