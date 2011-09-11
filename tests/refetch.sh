#!/bin/sh

if test "$1" == ""; then
    echo "Usage: $0 [animea|mangareader]"
    exit 1
fi

cat <<EOF | grep "^$1" |
animea_results_empty_html	http://manga.animea.net/search.html?title=rtzreft
animea_results_html	http://manga.animea.net/search.html?title=
animea_papillon_c1_p1_html	http://manga.animea.net/papillon-hana-to-chou-chapter-1-page-1.html
animea_papillon_c1_p2_html	http://manga.animea.net/papillon-hana-to-chou-chapter-1-page-2.html
animea_papillon_chapters_html	http://manga.animea.net/papillon-hana-to-chou.html
mangareader_results_empty_html	http://www.mangareader.net/search/?w=trzrt
mangareader_results_html	http://www.mangareader.net/search/?w=&p=30
mangareader_goong_c1_p1_html	http://www.mangareader.net/462-28574-1/goong/chapter-1.html
mangareader_goong_c1_p2_html	http://www.mangareader.net/462-28574-2/goong/chapter-1.html
mangareader_goong_chapters_html	http://www.mangareader.net/462/goong.html
EOF

while read name url; do
    wget -O res/raw/$name $url
done
