start "RCSSServer" /D ..\Environment\rcssserver-14.0.3-win "rcssserver"
sleep 2
start "RCSSMonitor" /D ..\Environment\rcssmonitor-14.1.0-win "rcssmonitor"
sleep 2
start "RCSSLogger" /D ..\Environment\Logger "LoggingCommands"
sleep 2
start "Krislet" /D ..\ClassicKrislet "TeamStart"