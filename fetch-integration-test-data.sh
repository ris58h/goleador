#!/bin/sh

format=$1
dir="test/video/${format}"

if [ ! -d $dir ]; then
    mkdir -p $dir
fi

for video_id in "cLjn6oF1E9Q" "D6hdF7gChmE" "KyW4keXAT3s" "ZdFEZlepWJI" ; do
    youtube-dl -f $format "https://www.youtube.com/watch?v=${video_id}" -o "${dir}/%(id)s.%(ext)s"
done