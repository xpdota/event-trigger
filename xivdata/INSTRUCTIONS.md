### Icons

```
# Extract icons
./SaintCoinach.Cmd.exe  'C:\Program Files (x86)\Steam\steamapps\common\FINAL FANTASY XIV Online' ui
# Move them to one folder
mkdir combined
mv whatever_path/*/*.png combined/
# Preview needed icons
# Action.csv = relevant data file, -f = which field represents the icon
# Just kidding, this doesn't properly handle commas in quotes
cut -f 4 -d, Action.csv | sort -n | uniq
# Actually copy them to a new folder
mkdir actions
cut -f 4 -d, Action.csv | sort -n | uniq | grep -E '[0-9]+' |  awk '{printf "%06d\n", $0}' | xargs -n 1 -I '{}' mv '2021.08.17.0000.0000/ui/icon/combined/{}.png' actions/
#                                          ^ filters out header             ^ left pad          ^ actual move

# Actually working versions: Run ActionIcon.main or StatusEffectIcon.main, then:
stuff | xargs -n 1 -I '{}' cp '2021.08.17.0000.0000/ui/icon/combined/{}.png' actions/
```