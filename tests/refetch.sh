#!/bin/sh

if test "$1" == ""; then
    echo "Usage: $0 [animea|mangareader|naver]"
    exit 1
fi

cat <<EOF | grep "^$1" |
animea_results_empty_html	http://manga.animea.net/search.html?title=rtzreft
animea_results_html	http://manga.animea.net/search.html?title=
animea_papillon_c1_p1_html	http://manga.animea.net/papillon-hana-to-chou-chapter-1-page-1.html
animea_papillon_c1_p2_html	http://manga.animea.net/papillon-hana-to-chou-chapter-1-page-2.html
animea_papillon_chapters_html	http://manga.animea.net/papillon-hana-to-chou.html
animea_advanced_search          http://manga.animea.net/search.html?title=&completed=0&yor_range=0&yor=&type=any&author=&artist=&genre%5BAction%5D=1&genre%5BAdventure%5D=0&genre%5BComedy%5D=0&genre%5BDoujinshi%5D=0&genre%5BDrama%5D=0&genre%5BEcchi%5D=2&genre%5BFantasy%5D=1&genre%5BGender_Bender%5D=0&genre%5BHarem%5D=0&genre%5BHistorical%5D=0&genre%5BHorror%5D=0&genre%5BJosei%5D=0&genre%5BMartial_Arts%5D=0&genre%5BMature%5D=2&genre%5BMecha%5D=0&genre%5BMystery%5D=0&genre%5BPsychological%5D=0&genre%5BRomance%5D=0&genre%5BSchool_Life%5D=0&genre%5BSci-fi%5D=0&genre%5BSeinen%5D=0&genre%5BShotacon%5D=0&genre%5BShoujo%5D=0&genre%5BShoujo_Ai%5D=0&genre%5BShounen%5D=0&genre%5BShounen_Ai%5D=0&genre%5BSlice_of_Life%5D=0&genre%5BSmut%5D=0&genre%5BSports%5D=0&genre%5BSupernatural%5D=0&genre%5BTragedy%5D=0&genre%5BYaoi%5D=0&genre%5BYuri%5D=0#results
mangareader_results_empty_html	http://www.mangareader.net/search/?w=trzrt
mangareader_results_html	http://www.mangareader.net/search/?w=&p=30
mangareader_goong_c1_p1_html	http://www.mangareader.net/462-28574-1/goong/chapter-1.html
mangareader_goong_c1_p2_html	http://www.mangareader.net/462-28574-2/goong/chapter-1.html
mangareader_goong_chapters_html	http://www.mangareader.net/462/goong.html
mangareader_advanced_search     http://www.mangareader.net/search/?w=&rd=0&status=&order=&genre=2000020000200002200001000020002000020&p=0
naver_results_empty_html	http://comic.naver.com/search.nhn?m=webtoon&keyword=iufioasjfdoasdf
naver_results_html		http://comic.naver.com/search.nhn?m=webtoon&keyword=%ED%95%91%ED%81%AC%EB%A0%88%EC%9D%B4%EB%94%94
naver_pink_lady_chapters_html	http://comic.naver.com/webtoon/list.nhn?titleId=22896
naver_pink_lady_c1_p1_html		http://comic.naver.com/webtoon/detail.nhn?titleId=22896&no=1&weekday=mon
EOF

while read name url; do
    wget -O res/raw/$name $url
done
