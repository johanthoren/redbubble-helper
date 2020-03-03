# Introduction to Redbubble Helper

## What is it?

It's a tool to transform a plain text based SVG with black letters into multiple PNGs suitable for upload to Redbubble. It might seem like a small thing to do manually, but when handling hundreds or thousands of designs, every minute saved matters.

It is based around the assumption that the design is single colored, although it should work OK in other cases as well. I personally only use text in my designs, so that's what works the best.

`rbh` simply takes an SVG and produces the following:
- a big, padded PNG file with the same basic layout as the original.
- a version of that PNG that is rotated 90 degrees.
- another version of that PNG that is rotated 270 degrees.
- an SVG where black text has been turned white
- all the PNGs above but based on the white version.

If it's saving you some time, feel free to buy something from [my store](https://www.redbubble.com/people/kebab-case/shop "Johan Thorén's Redbubble Shop") or send a donation through [paypal](https://paypal.me/johanthoren?locale.x=en_US "Johan Thorén's Paypal").

## Dependencies

- ImageMagick 6
- Inkscape
- JDK 8 or later
- Bash

It's only developed and tested on Linux, but it will likely run just fine on MacOS.

## Installation

Build with:
```
lein uberjar
lein bin
```

## Usage

```
$ rbh [OPTIONS] file ...
```

## Options

```
-d, --debug    Sets log level to debug
-h, --help     Print this help message
-v, --version  Print the current version number of rbh.
```

## Known issues

The default limits in ImageMagick need to be increased.

Standard `/etc/ImageMagick-6/policy.xml` excerpt:
```
...
  <policy domain="resource" name="memory" value="256MiB"/>
...
```

The following works for me:
```
...
  <policy domain="resource" name="memory" value="2GiB"/>
...
```

Your milage may vary.

For more information, see [the ImageMagick documentation](http://www.imagemagick.org/script/resources.php "ImageMagick resource documentation").
