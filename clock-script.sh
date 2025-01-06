#!/bin/bash

span() {
  printf '<span color="%s">%s</span>' "$2" "$1"
}

time=$(span "$(date +'%-I:%M:%S %P')" "#8caaee")
date_text="$(date +'%m.%d.%Y')"
date=$(span "${date_text}" "#ca9ee6")

declare cal
if [ ! -f "$HOME/.config/waybar/date_store.dat" ] || [ ! "${date_text}" = "$(cat "$HOME/.config/waybar/date_store.dat")" ]; then
  cal="$(cal --week -m --color=always | cat -A)"
  cal=${cal/"^[[7m"/'<span color="#ca9ee6">'}
  cal=${cal/"^[[0m"/"</span>"}
  cal=${cal//$/}
  echo "${date_text}" > "$HOME/.config/waybar/date_store.dat"
  printf "%s" "${cal}" > "$HOME/.config/waybar/tt_store.dat"
else
  cal=$(<"$HOME/.config/waybar/tt_store.dat")
fi

jq -n -c --unbuffered\
  --arg t "${time} ${date}" \
  --arg tt "${cal}
$(span ":click to open daily" "#e78284")" \
   '{text: $t, tooltip: $tt}'