::
:: This batch file serves as a convienance by filling in the username,
:: password, update action, id and file command line attributes. All the user
:: needs to specify is the event id to update and the file path with the updated
:: content.
::
:: Note - The entry id can be determined from the list action
::

:: Usage - ljupdate 77 "c:\foo\bar\blog.entry"

java -jar %LJTOOLKIT_HOME%\lib\ljtoolkit.jar -u username -p password -a update -id %1 -f %2