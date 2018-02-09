# sciencefair

A science fair web site.   Built for the first science fair for the Groton Dunstable Elementary School.  Mostly for sharing information and registration.

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Markdown

To generate the HTML pages from markdown templates:

    lein exec -p markdown.clj

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2014 Bob Herrmann

