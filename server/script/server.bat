@echo off
rem
rem Copyright (c) OrientDB LTD (http://www.orientdb.com)
rem

echo            .                                          
echo           .`        `                                 
echo           ,      `:.                                  
echo          `,`    ,:`                                   
echo          .,.   :,,                                    
echo          .,,  ,,,                                     
echo     .    .,.:::::  ````                                 :::::::::     :::::::::
echo     ,`   .::,,,,::.,,,,,,`;;                      .:    ::::::::::    :::    :::
echo     `,.  ::,,,,,,,:.,,.`  `                       .:    :::      :::  :::     :::
echo      ,,:,:,,,,,,,,::.   `        `         ``     .:    :::      :::  :::     :::
echo       ,,:.,,,,,,,,,: `::, ,,   ::,::`   : :,::`  ::::   :::      :::  :::    :::
echo        ,:,,,,,,,,,,::,:   ,,  :.    :   ::    :   .:    :::      :::  :::::::
echo         :,,,,,,,,,,:,::   ,,  :      :  :     :   .:    :::      :::  :::::::::
echo   `     :,,,,,,,,,,:,::,  ,, .::::::::  :     :   .:    :::      :::  :::     :::
echo   `,...,,:,,,,,,,,,: .:,. ,, ,,         :     :   .:    :::      :::  :::     :::
echo     .,,,,::,,,,,,,:  `: , ,,  :     `   :     :   .:    :::      :::  :::     :::
echo       ...,::,,,,::.. `:  .,,  :,    :   :     :   .:    :::::::::::   :::     :::
echo            ,::::,,,. `:   ,,   :::::    :     :   .:    :::::::::     ::::::::::
echo            ,,:` `,,.                                  
echo           ,,,    .,`                                  
echo          ,,.     `,
echo        ``        `.                                         
echo                  ``                                         www.orientdb.com
echo                  `                                    

rem Guess YOUTRACKDB_HOME if not defined
set CURRENT_DIR=%cd%

if exist "%JAVA_HOME:"=%\bin\java.exe" goto setJavaHome
set JAVA=java
goto okJava

:setJavaHome
set JAVA="%JAVA_HOME:"=%\bin\java"

:okJava
if not "%YOUTRACKDB_HOME%" == "" goto gotHome
set YOUTRACKDB_HOME=%CURRENT_DIR%
if exist "%YOUTRACKDB_HOME%\bin\server.bat" goto okHome
cd ..
set YOUTRACKDB_HOME=%cd%
cd %CURRENT_DIR%

:gotHome
if exist "%YOUTRACKDB_HOME%\bin\server.bat" goto okHome
echo The YOUTRACKDB_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end

:okHome
rem Get remaining unshifted command line arguments and save them in the
set CMD_LINE_ARGS=

:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs

:doneSetArgs


if NOT exist "%CONFIG_FILE%" set CONFIG_FILE=%YOUTRACKDB_HOME%/config/youtrackdb-server-config.xml

set LOG_FILE=%YOUTRACKDB_HOME%/config/youtrackdb-server-log.properties
set WWW_PATH=%YOUTRACKDB_HOME%/www
set ORIENTDB_SETTINGS=-Dprofiler.enabled=true
set JAVA_OPTS_SCRIPT= -Djna.nosys=true -XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true -Dfile.encoding=UTF8 -Drhino.opt.level=9

rem TO DEBUG ORIENTDB SERVER RUN IT WITH THESE OPTIONS:
rem -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044
rem AND ATTACH TO THE CURRENT HOST, PORT 1044

rem ORIENTDB MAXIMUM HEAP. USE SYNTAX -Xmx<memory>, WHERE <memory> HAS THE TOTAL MEMORY AND SIZE UNIT. EXAMPLE: -Xmx512m
set MAXHEAP=-Xms2G -Xmx2G
rem ORIENTDB MAXIMUM DISKCACHE IN MB, EXAMPLE: "-Dstorage.diskCache.bufferSize=8192" FOR 8GB of DISKCACHE
set MAXDISKCACHE=

call %JAVA% -server %JAVA_OPTS% %MAXHEAP% %JAVA_OPTS_SCRIPT% %ORIENTDB_SETTINGS% %MAXDISKCACHE% -Djava.util.logging.manager=com.orientechnologies.common.log.ShutdownLogManager -Djava.util.logging.config.file="%LOG_FILE%" -Dyoutrackdb.config.file="%CONFIG_FILE%" -Dorientdb.www.path="%WWW_PATH%" -Dorientdb.build.number="@BUILD@" -cp "%YOUTRACKDB_HOME%\lib\*;%YOUTRACKDB_HOME%\plugins\*" %CMD_LINE_ARGS% com.orientechnologies.orient.server.OServerMain

:end
