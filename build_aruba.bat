for /d /r %%x in (crunch) do rd /s /q "%%x"
ant release -buildfile d:\files\proxibase\code\aircandi\aruba\build.xml
pause