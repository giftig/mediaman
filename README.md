# Mediaman

## Overview

This project is designed to manage a TV programme / movie library via a
RESTful HTTP interface. It is written in Scala / Spray HTTP and provides a
simple interface for uploading and downloading episodes of TV programmes, as
well as creating new programmes. Programmes are stored in a format like:

    My Awesome Programme/S01 E02.mkv


The /programme and /episode endpoints accept data to create named TV programmes
and to upload a specific episode of an existing TV programme respectively. The
service can be configured to allow simple authentication, to allow various
different formats, maximum file sizes, etc.

### Scope

Though it'd be useful to provide a interface to index the library, delete
unwanted programmes or episodes, etc., that is not currently a priority as I
have already written a Flask project to do that for me. I may add / move that
functionality to here at a later point if I achieve my other goals for this
project.


### Origin

I created this project largely as a means of playing around with Scala and
Spray, more than to perform a useful function, but it nevertheless performs
a sufficiently useful function to be worth sharing, I think.


### Admin interface

The service also runs an HTTP admin interface on a different port for
convenience. It should be configured to run on an interface which isn't exposed
to unauthorised users, as it doesn't use any form of authentication and can be
used to get some simple service status information, and to shut down the
service.

I plan to also collect some stats about the service's usage and report them at
the /stats endpoint; that's a work in progress.

## Running the project

Configure development.conf as desired; especially make sure that `media.path`
exists and is accessible. Then just run the project with `mvn scala:run`.

You can test the endpoints using curl, for example:

```bash
curl http://localhost:8200/status

curl \
  -X POST \
  -u 'arya:stickthemwiththepointyend' \
  -d 'name=Test+Programme' \
  http://localhost:8100/programme

curl \
  -u 'arya:stickthemwiththepointyend' \
  -X PUT \
  -F 'programme=Test Programme' \
  -F 'season=1' \
  -F 'episode=3' \
  -F 'file=@testfile.mp4' \
  http://localhost:8100/episode

curl -X POST http://localhost:8200/stop
```

You can build a fat JAR for production use with the command

```bash
  scripts/build.sh -c [production config] -l [optional production logback.xml]
```

## Issues

I'm currently considering these issues:

  * Lack of HTTPS support: though I'm personally only running it on a private
    network, I ought to use HTTPS, especially with the use of Spray's BasicAuth,
    as otherwise passwords are sent as plaintext.
  * Improper chunking: the uploads don't use chunking properly, instead sending
    data as one big load and writing it to a file in chunks. The file seems to
    be loaded into memory before the request completes though, even when the
    request fails verification and the service never attempts to save it. See
    [this thread](https://groups.google.com/forum/#!topic/spray-user/mtrj_zTRqz0)
  * I'm currently missing unit tests for a couple of recent additions (the
    admin interface and the Reaper actor). These will be added as soon as I get
    around to writing them.
