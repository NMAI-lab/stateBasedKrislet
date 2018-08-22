start "RCSSServer" /D \rcssserver-14.0.3-win\ "rcssserver"
sleep 2
start "RCSSMonitor" /D \rcssmonitor-14.1.0-win\ "rcssmonitor"
sleep 2
start "RCSSLogger" /D \Logger\ "LoggingCommands"
sleep 2
::start "Krislet" /D ..\ClassicKrislet\ "TeamStart"
::start "Krislet" /D ..\FiniteTurnKrislet\ "TeamStart"
start "Krislet" /D ..\StateBasedFiniteTurnKrislet\ "TeamStart"