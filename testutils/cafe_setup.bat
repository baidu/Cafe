adb shell service call window 2
adb shell service call window 1 i32 4939
adb shell ps | findstr "monkey" > a.inf
for /f "eol=; tokens=2 delims=, " %%i in (a.inf) do @adb shell kill %%i
del a.inf
adb shell monkey --port 4938 --ignore-crashes --ignore-security-exceptions --ignore-native-crashes -v -v
pause
