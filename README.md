# TechnionSlideshow
This is the app I made to allow an infinite slideshow taking pics from Google Drive folders 
to run on Android Stick MK808 running android 4.2.

Because this stick runs an old version of android, it has a problem processing jpeg files with EXIF data.
Therefore, the pics need to be pre-processed as follows, before the app can run without a problem (if not, the app will throw an exception
with this detailed information in the textbox)

Please pay close attention to the following information:
                                "Some pictures in the selected folder contain EXIF metadata, 
                                "and this device, running an older version of Android, 
                                "cannot decode those pictures, so the slideshow cannot start.
                                "Please clear ALL EXIF data from pictures and try again.
                                "Here is one way to do so:
                                "Go to www.ImageMagick.org and download ImageMagick,
                                "extract it and open  CMD from within the folder,
                                "then run the following command:
                                "mogrify -auto-orient -strip <folder of pictures>\\*.jpg
                                "Afterwards, upload them again to Google Drive.
                                "This command OVERWRITES the files, auto-rotates them
                                "and then removes the EXIF metadata.
                                "If you don't want to overwrite, use:
                                "convert -auto-orient -strip <original folder of pictures>\\*.jpg 
                                "<target folder of pictures>\\%04d.jpg
                                "or consult the documentation of \"convert\" or \"mogrify\" commands
                                "at http://www.imagemagick.org/script/command-line-tools.php
