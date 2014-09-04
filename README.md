next-action-clojure
===================

A clojure application bringing [GTD](http://en.wikipedia.org/wiki/Getting_Things_Done) in Todoist.
Adds the support for **next actions** as weel as **Someday** inbox.

By default all projects are sequentials, meaning a project tasks are executed one after the other.

In addition, if the project name ends with *parallel-postfix*, the project will be parallel,
meaning all the task of this project can be executed (there is no order).

A project can also be considered as a list of task if its name ends with *list-postfix* as therefor should be ignored (used for the grocery list by example).

Finally, the project named *someday-label* will be ignored as well, since it contains the list of non-active projects.

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed if you want to run the code in development mode.
Otherwise, you only need a JRE to run the **jar** file.

You will also need [Todoist][2] as well as your Todoist API token (can be found under Settings -> Account).

[1]: https://github.com/technomancy/leiningen
[2]: https://todoist.com

## Download

Download the [jar file](https://github.com/BenjaminVanRyseghem/next-action-clojure/raw/master/target/next-action-1.0.0-standalone.jar) for a standalone application.

Otherwise, just clone/fork this repository.

## Running

To start the jar file, run:

    $ java -jar next-action-1.0.0-standalone.jar

If you want to run the development code, clone this repository then run:

    lein run

You also need to create a `info.json` file which must be in the directory where you run the Java command.

## Compiling

To compile the application as a stand-alone application, run:

    lein uberjar

The generated jars can be found in `./target`

You can then run it using:

    $ java -jar next-action-1.0.0-standalone.jar

## info.json structure

The `info.json` file is structured like this: 

    {
    	"api-token": "String - Todoist API token. The API token is accessible in Settings -> Account",
    	"someday-label": "String - Label of your GTD `Someday` project",
    	"list-prefix": "String - Prefix for your projects that should be considered as lists",
    	"next-action-label": "String - Label name for your next action tasks. If it does not exists, it will be created",
    	"parallel-postfix": "String - Postfix indicating a project should handle its tasks in parallel way",
    	"sleeping-time": Int - Time between to iterations 
    }

## Credits

This project is inspired by [NextAction](https://github.com/akramer/NextAction) by Adam Kramer.

## License

Copyright © 2014 Benjamin Van Ryseghem

Distributed under the General Public License (GPL) v3.0.
See `LICENSE` for more information about the licensing of this project.
