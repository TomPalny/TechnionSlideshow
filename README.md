# TechnionSlideshow
This is the app I made to allow an infinite slideshow taking pics from Google Drive folders 
to run on Android Stick MK808 running android 4.2.

Because this stick runs an old version of android, it has a problem decoding jpeg files with EXIF data.
Therefore, the pics need to be pre-processed as described below before the app can run without a problem. If not, the app will throw an exception with the detailed information on how to process the images correctly.

                                Here is one way to do so:
                                Go to www.ImageMagick.org and download ImageMagick,
                                extract it and open  CMD from within the folder,
                                then run the following command
                                (This command OVERWRITES the files, auto-rotates them
                                and then removes the EXIF metadata):
                                
                                mogrify -auto-orient -strip -resize 1920 <folder of pictures>\*.jpg
                                
                                Afterwards, upload them again to Google Drive.
                                
                                If you don't want to overwrite, use:
                                convert -auto-orient -strip -resize 1920 <original folder of pictures>\*.jpg 
                                <target folder of pictures>\%04d.jpg
                                or consult the documentation of "convert" or "mogrify" commands
                                at http://www.imagemagick.org/script/command-line-tools.php
