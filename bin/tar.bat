@echo off
rem tar -ca <rel or abs path>
rem tar -xa tarfile
set "action=%1"
shift
set "src=%~f1"
shift
set "args="
set "add="
:start
if [%1] == [] goto end
if [%1] neq [%action%] set add=1
if [%1] neq [%src%] set add=1
if defined add set args=%args%%1
shift
goto start
:end
java -jar D:\bin\archive.jar -g %action% %src% %args%