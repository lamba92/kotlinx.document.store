@echo off
for /r %%i in (.) do (
    if /i "%%~nxi"==".gradle" (
        echo Deleting: %%i
        rd /s /q "%%i"
    )
    if /i "%%~nxi"=="build" (
        echo Deleting: %%i
        rd /s /q "%%i"
    )
)
