# brady

the monorepo!

## required patch

do note there is a required API patch for MCCoroutine to work on SportPaper. You can find it in `patches/`!
you can find a SportPaper with this in it [here](https://github.com/PGMFFEnthusiasts/SportPaper) that's
specifically for brady.

## adding more plugins

you probably actually want your code to rely on the `deps` plugin or the `core` plugin. `deps` is nice since
we can just shade all the dependencies into it and know that at runtime, it will have everything loaded properly.
don't forget to add your dependencies to `deps` if they aren't already there.

keep in mind though if this clashes with the classpath of some of the mainline PGM plugins (looking at you,
Community), that you'd probably want to load those beforehand. awful!

## this is jank and awful

monorepo
