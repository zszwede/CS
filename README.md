###TASK
Our custom-build server logs different events to a file. Every event has 2 entries in a log - one entry when the event was started and another when the event was finished. The entries in a log file have no specific order (it can occur that a specific event is logged before the event starts)
Every line in the file is a JSON object containing event data:
- id - the unique event identifier
- state - whether the event was started or finished (can have values "STARTED" or "FINISHED"
- timestamp - the timestamp of the event in milliseconds
Application Server logs also have the additional attributes:
- type - type of log
- host - hostname

###The program should:
- Take the input file path as input argument
- Flag any long events that take longer than 4ms with a column in the database called "alert"
- Write the found event details to file-based HSQLDB (http://hsqldb.org/) in the working folder
- The application should create a new table if necessary and enter the following values:
    - Event id
    - Event duration
    - Type and Host if applicable
    - "alert" true is applicable
- Additional points will be granted for:
    - Proper use of info and debug logging
    - Proper use of Object Oriented programming
    - Unit test coverage
    - Multi-threaded solution
    - Program that can handle very large files (gigabytes)
   
### How to run solution

How to build application
```
./gradlew jar
```

How to run application
```
cd build/libs
```
```
usage: java -jar CS-1.0.jar [-c <config file>] -f <application log> [-r]
 -c,--config <config file>     Override configuration file [default:config.properties]
 -f,--file <application log>   Path of the file to read/tail
 -r,--results                  Show results (>4ms) after completion
```

Example: Read app.log using new_config.properties and display results on the end
```
java -jar CS-1.0.jar -f app.log -c new_config.properties -r
```

Large files may require overriding thread/pool values in config properties and JVM changing memory settings. 
