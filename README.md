# Jengamon's Launchpad X for Bitwig 

![Java CI](https://github.com/Jengamon/Launchpad-X-Bitwig-Script/workflows/Java%20CI/badge.svg?branch=api-10)

## Why not DrivenByMoss?

I love the work that Moss has done, and I would switch between the two scripts.
It's just that I wanted a more tactile version of the script, that more
took advantage for the hardware's features rather than replace them.

Basically, I wanted it to work almost exactly like it does in the manual.

There are some changes, which I probably should document, but nahhh for now.

This script is much cleaner than my Mini Mk3 script. I might port over the Mk3
to this framework, but we'll see.

## TODO

- Add Mac and Linux autodetection (maybes take this from DrivenByMoss?)

## API 10 Version

It seems that the JS API for Bitwig just has some limitiations that I can't jive
with, so I made the switch to the Java API.

It's actually going pretty well. The script is partially working now, and it's an
exciting time again.

## Installation

Simply download the desired version of "LaunchpadX.bwextension" from the Releases page,
then put it in your Bitwig "Extensions" folder.

Or you can build it yourself (and at the bleeding edge) by downloading the repository
and running "mvn install" in the root directory with both JDK (at least 12) and 
Maven installed.