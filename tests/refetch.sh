#!/bin/sh

if test "$1" == ""; then
    echo "Usage: $0 [animea|mangareader|naver|mangahere]"
    exit 1
fi

cat <<EOF | grep "^$1" |
animea_results_empty_html	http://manga.animea.net/search.html?title=rtzreft
animea_results_html	http://manga.animea.net/search.html?title=&page=2
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
mangahere_results_empty_html	http://www.mangahere.com/search.php?direction=&name_method=cw&name=trzt
mangahere_results_html		http://www.mangahere.com/search.php?direction=&name_method=cw&name=ab
mangahere_rinne_c2_p1_html	http://www.mangahere.com/manga/kyoukai_no_rinne/v01/c002/
mangahere_rinne_c2_p2_html	http://www.mangahere.com/manga/kyoukai_no_rinne/v01/c002/2.html
mangahere_rinne_chapters_html	http://www.mangahere.com/manga/kyoukai_no_rinne/
mangahere_advanced_search	http://www.mangahere.com/search.php?direction=&name_method=cw&name=a&author_method=cw&author=&artist_method=cw&artist=&genres%5BAction%5D=0&genres%5BAdventure%5D=0&genres%5BComedy%5D=0&genres%5BDoujinshi%5D=0&genres%5BDrama%5D=0&genres%5BEcchi%5D=0&genres%5BFantasy%5D=1&genres%5BGender+Bender%5D=0&genres%5BHarem%5D=0&genres%5BHistorical%5D=0&genres%5BHorror%5D=0&genres%5BJosei%5D=0&genres%5BMartial+Arts%5D=0&genres%5BMature%5D=0&genres%5BMecha%5D=0&genres%5BMystery%5D=0&genres%5BOne+Shot%5D=2&genres%5BPsychological%5D=1&genres%5BRomance%5D=0&genres%5BSchool+Life%5D=0&genres%5BSci-fi%5D=0&genres%5BSeinen%5D=0&genres%5BShoujo%5D=0&genres%5BShoujo+Ai%5D=0&genres%5BShounen%5D=0&genres%5BShounen+Ai%5D=0&genres%5BSlice+of+Life%5D=0&genres%5BSports%5D=0&genres%5BSupernatural%5D=0&genres%5BTragedy%5D=0&released_method=eq&released=&is_completed=&advopts=1
EOF

while read name url; do
    wget --user-agent="MangaGet/1.0" -O res/raw/$name $url
done
