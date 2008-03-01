::
:: This batch file serves as a convienance by filling in the username,
:: password, add action and file command line attributes. All the user
:: needs to specify is the file path

:: Usage - ljadd "c:\foo\bar\blog.entry"

java -jar %LJTOOLKIT_HOME%\lib\ljtoolkit.jar -u username -p password -a add -f %1