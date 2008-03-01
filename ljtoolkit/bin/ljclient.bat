::
:: This batch file serves as a convienance by filling in the username
:: and password command line attributes. The user is responsible for
:: filling out the rest of the command line options appropriately.
::

:: Example - ljclient -a add -f "c:\foo\bar\blog.enry"

java -jar %LJTOOLKIT_HOME%\lib\ljtoolkit.jar -u username -p password %*