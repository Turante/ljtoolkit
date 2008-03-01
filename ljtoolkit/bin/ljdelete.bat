::
:: This batch file serves as a convienance by filling in the username,
:: password, delete action and id command line attributes. All the user
:: needs to specify is Live Journal event id (integer) to for the event
:: to be deleted.
::
:: Note - The entry id can be determined from the list action
::

:: Usage - ljdelete 77

java -jar %LJTOOLKIT_HOME%\lib\ljtoolkit.jar -u username -p password -a delete -id %1