### Icons

```
# Extract icons
./SaintCoinach.Cmd.exe  'C:\Program Files (x86)\Steam\steamapps\common\FINAL FANTASY XIV Online' uihd
# Move them to one folder
cd 2021.11.28.xxxx.yyyy/ui/icon
mkdir combined
mv */*.png combined/
mkdir actions/
# Run ActionIcon.main or StatusEffectIcon.main, then 
java <stuff> | xargs -n 1 -I '{}' cp 'combined/{}_hr1.png' actions/
# or just be lazy and copy and paste
cat | xargs -n 1 -I '{}' cp 'combined/{}_hr1.png' actions/
# (paste after starting the command, then ^D to end)

```