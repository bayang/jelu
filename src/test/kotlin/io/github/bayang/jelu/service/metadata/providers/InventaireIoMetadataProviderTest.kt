package io.github.bayang.jelu.service.metadata.providers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataDto
import io.github.bayang.jelu.dto.MetadataRequestDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestClient

@SpringBootTest
class InventaireIoMetadataProviderTest(
    @Autowired private val springRestClient: RestClient,
    @Autowired private val buildProperties: BuildProperties,
) {

    @Test
    fun fetchMetadata_fromCorrectIsbn_returnsBookMetaDataDto() {
        val builder = springRestClient.mutate()
        val serv = MockRestServiceServer.bindTo(builder).build()
        val isbn = MockRestResponseCreators.withSuccess().body(
            """
                              {"entities":{"isbn:9782290349229":{"_id":"d59e3e64f92c6340fbb10c5dcf7c0abf","_rev":"4-2db713fd44c6ade760f623367ec7c33b","type":"edition","labels":{"fromclaims":"L'homme aux cercles bleus"},"claims":{"wdt:P31":["wd:Q3331189"],"wdt:P212":["978-2-290-34922-9"],"wdt:P957":["2-290-34922-4"],"wdt:P407":["wd:Q150"],"wdt:P1476":["L'homme aux cercles bleus"],"wdt:P629":["wd:Q3203603"],"wdt:P123":["wd:Q3156592"],"invp:P2":["57883743aa7c6ad25885a63e6e94349ec4f71562"],"wdt:P577":["2005-05-01"],"wdt:P1104":[220],"wdt:P2969":["1508217"]},"created":1485023383338,"updated":1668681738527,"version":6,"uri":"isbn:9782290349229","originalLang":"fr","image":{"url":"/img/entities/57883743aa7c6ad25885a63e6e94349ec4f71562"},"invId":"d59e3e64f92c6340fbb10c5dcf7c0abf"}},"redirects":{}}
                        """,
        )
        val edition = MockRestResponseCreators.withSuccess().body(
            """
                {"entities":{"wd:Q3203603":{"uri":"wd:Q3203603","wdId":"Q3203603","type":"work","labels":{"fr":"L'Homme aux cercles bleus","en":"The Chalk Circle Man","it":"L'uomo dei cerchi azzurri","ja":"青チョークの男","nl":"The Chalk Circle Man","de":"Es geht noch ein Zug von der Gare du Nord","ko":"파란 동그라미의 사나이","ga":"L'Homme aux cercles bleus"},"aliases":{"fr":["L'homme aux cercles bleus","L'Homme Aux Cercles Bleus"]},"descriptions":{"it":"romanzo scritto da Fred Vargas","en":"1991 mystery novel by Fred Vargas, 1st of her Detective Adamsberg series","de":"Mystery-Kriminalroman von Fred Vargas (1991), erster Roman der Adamsberg-Reihe","fr":"roman de Fred Vargas","nl":"boek van Fred Vargas","he":"ספר","es":"libro de Fred Vargas","br":"romant","ko":"프레드 바르가스의 소설","ar":"عمل مكتوب"},"sitelinks":{"enwiki":{"title":"The Chalk Circle Man","badges":[]},"frwiki":{"title":"L'Homme aux cercles bleus","badges":[]},"itwiki":{"title":"L'uomo dei cerchi azzurri","badges":[]},"jawiki":{"title":"青チョークの男","badges":[]}},"claims":{"wdt:P31":["wd:Q47461344","wd:Q7725634"],"wdt:P136":["wd:Q182015","wd:Q5937792"],"wdt:P179":["wd:Q27536277"],"wdt:P407":["wd:Q150"],"wdt:P577":["1991"],"wdt:P921":["wd:Q484188"],"wdt:P7937":["wd:Q8261"],"wdt:P50":["wd:Q237087"],"wdt:P674":["wd:Q1684606"],"wdt:P840":["wd:Q90"],"wdt:P1476":["L'Homme aux cercles bleus"],"wdt:P156":["wd:Q3203802"],"wdt:P166":["wd:Q2425126"],"wdt:P1545":["1"]},"originalLang":"fr","lastrevid":2193779480}},"redirects":{}}
            """,
        )
        val author = MockRestResponseCreators.withSuccess().body(
            """
                {"entities":{"wd:Q237087":{"uri":"wd:Q237087","wdId":"Q237087","type":"human","labels":{"pt":"Fred Vargas","fr":"Fred Vargas","en":"Fred Vargas","es":"Fred Vargas","ca":"Fred Vargas","it":"Fred Vargas","et":"Fred Vargas","de":"Fred Vargas","nn":"Fred Vargas","ja":"フレッド・ヴァルガス","cs":"Fred Vargas","fa":"فرد وارگا","bg":"Фред Варгас","mzn":"فرد وارگا","nl":"Fred Vargas","da":"Fred Vargas","sv":"Fred Vargas","zh":"弗雷德·瓦格斯","ckb":"فرێد ڤارگاس","nb":"Fred Vargas","sl":"Fred Vargas","fi":"Fred Vargas","hu":"Fred Vargas","pl":"Fred Vargas","azb":"فرد وارقا","eu":"Fred Vargas","br":"Fred Vargas","gl":"Fred Vargas","uk":"Фред Варґас","ast":"Fred Vargas","ru":"Фред Варгас","ar":"فرد فارغاس","pt-br":"Fred Vargas","cy":"Fred Vargas","sq":"Fred Vargas","arz":"فرد فارجاس","pap":"Fred Vargas","ka":"ფრედ ვარგასი","tr":"Fred Vargas"},"aliases":{"fr":["Frédérique Audoin-Rouzeau"],"es":["Frederique Audoin-Rouzeau","Frédérique Audoin-Rouzeau","Fredérique Audoin-Rouzeau","Frédérique Audoin Rouzeau","Frederique Audoin Rouzeau","Fredérique Audoin Rouzeau"],"et":["Frédérique Audoin-Rouzeau"],"cs":["Frédérique Audouin-Rouzeau","Fredérique Vargas"],"sv":["Vargas"],"nb":["Frédérique Audouin-Rouzeau","Frederique Audouin-Rouzeau"],"en":["Frédérique Audoin-Rouzeau"],"it":["Frédérique Audoin-Rouzeau"],"uk":["Фредерік Одуан-Рузо"],"de":["Frédérique Audoin-Rouzeau"],"pl":["Frédérique Audoin-Rouzeau"],"zh":["弗蕾德·瓦尔加斯"],"ru":["Варгас, Фред"]},"descriptions":{"it":"scrittrice francese di romanzi gialli e archeologa","fr":"archéozoologue et romancière française","de":"französische Krimi-Autorin und Archäologin","nl":"Frans schrijfster","en":"French writer, archaeologist and historian","fa":"نویسنده و دیرین‌شناس فرانسوی","br":"skrivagnerez gall","he":"סופרת צרפתייה","gl":"escritora francesa","es":"escritora francesa","ca":"escriptora francesa","ar":"كاتبة فرنسية","fi":"ranskalainen kirjailija","bn":"ফরাসি লেখিকা","pl":"francuska pisarka i archeolog","ro":"scriitoare franceză","et":"Prantsusmaa kirjanik","en-gb":"French writer","sq":"shkrimtare franceze","en-ca":"French writer","pt":"Escritora francesa","ga":"scríbhneoir Francach","hu":"francia történész, régész és regényírónő","dv":"ލިޔުންތެރިއެއް","cs":"francouzská spisovatelka","ast":"escritora francesa","eu":"idazle frantziarra","nn":"fransk skribent","sv":"fransk författare","da":"fransk skribent","uk":"французька письменниця","zh":"法国历史学家、考古学家、作家","ja":"フランスの小説家 (1957 - )","tr":"Fransız yazar (d. 1957)"},"sitelinks":{"arwiki":{"title":"فرد فارغاس","badges":[]},"arzwiki":{"title":"فرد فارجاس","badges":[]},"astwiki":{"title":"Fred Vargas","badges":[]},"azbwiki":{"title":"فرد وارقا","badges":[]},"bgwiki":{"title":"Фред Варгас","badges":[]},"cawiki":{"title":"Fred Vargas","badges":[]},"ckbwiki":{"title":"فرێد ڤارگاس","badges":[]},"commonswiki":{"title":"Category:Fred Vargas","badges":[]},"cswiki":{"title":"Fred Vargas","badges":[]},"cswikiquote":{"title":"Fred Vargas","badges":[]},"dawiki":{"title":"Fred Vargas","badges":[]},"dewiki":{"title":"Fred Vargas","badges":[]},"enwiki":{"title":"Fred Vargas","badges":[]},"eswiki":{"title":"Fred Vargas","badges":[]},"eswikiquote":{"title":"Fred Vargas","badges":[]},"etwiki":{"title":"Fred Vargas","badges":[]},"etwikiquote":{"title":"Fred Vargas","badges":[]},"euwiki":{"title":"Fred Vargas","badges":[]},"fawiki":{"title":"فرد وارگا","badges":[]},"fiwiki":{"title":"Fred Vargas","badges":[]},"frwiki":{"title":"Fred Vargas","badges":[]},"frwikiquote":{"title":"Fred Vargas","badges":[]},"glwiki":{"title":"Fred Vargas","badges":[]},"huwiki":{"title":"Fred Vargas","badges":[]},"itwiki":{"title":"Fred Vargas","badges":[]},"itwikiquote":{"title":"Fred Vargas","badges":[]},"jawiki":{"title":"フレッド・ヴァルガス","badges":[]},"kawiki":{"title":"ფრედ ვარგასი","badges":[]},"mznwiki":{"title":"فرد وارگا","badges":[]},"nlwiki":{"title":"Fred Vargas","badges":[]},"nnwiki":{"title":"Fred Vargas","badges":[]},"nowiki":{"title":"Fred Vargas","badges":[]},"plwiki":{"title":"Fred Vargas","badges":[]},"ptwiki":{"title":"Fred Vargas","badges":[]},"ruwiki":{"title":"Варгас, Фред","badges":[]},"slwiki":{"title":"Fred Vargas","badges":[]},"svwiki":{"title":"Fred Vargas","badges":[]},"ukwiki":{"title":"Фред Варґас","badges":[]},"zhwiki":{"title":"弗雷德·瓦格斯","badges":[]}},"claims":{"wdt:P31":["wd:Q5"],"wdt:P135":["wd:Q3326717"],"wdt:P136":["wd:Q208505"],"wdt:P213":["000000036862981X","0000000120300340"],"wdt:P214":["86983699","166612748"],"wdt:P227":["121145344","1089332017"],"wdt:P244":["n95096250"],"wdt:P268":["12058359w"],"wdt:P269":["028828232"],"wdt:P349":["00884611"],"wdt:P569":["1957-06-07"],"wdt:P648":["OL6926243A"],"wdt:P950":["XX1217760"],"wdt:P1006":["071874097"],"wdt:P1412":["wd:Q150"],"wdt:P2607":["5624dae0-2980-496a-9d14-bc7320800e3e"],"wdt:P2963":["68906"],"wdt:P3630":["3212"],"wdt:P5491":["2874"],"wdt:P7400":["vargasfred"],"wdt:P27":["wd:Q142"],"wdt:P69":["wd:Q999763"],"wdt:P103":["wd:Q150"],"wdt:P106":["wd:Q6625963","wd:Q3621491","wd:Q201788","wd:Q482980"],"wdt:P166":["wd:Q1320436","wd:Q2425126","wd:Q924392","wd:Q3332454"],"wdt:P22":["wd:Q3379257"],"wdt:P3373":["wd:Q2360290","wd:Q914690"],"wdt:P18":["Fred Vargas 2009.jpg"]},"originalLang":"fr","lastrevid":2234315588,"image":{"url":"https://commons.wikimedia.org/wiki/Special:FilePath/Fred%20Vargas%202009.jpg?width=1000","file":"Fred Vargas 2009.jpg","credits":{"text":"Wikimedia Commons","url":"https://commons.wikimedia.org/wiki/File:Fred Vargas 2009.jpg"}}}},"redirects":{}}
            """,
        )
        val genre1 = MockRestResponseCreators.withSuccess().body(
            """
               {"entities":{"wd:Q182015":{"uri":"wd:Q182015","wdId":"Q182015","labels":{"zh-hans":"惊悚","zh-hant":"驚悚","zh-hk":"驚慄","zh-cn":"惊悚","zh-sg":"惊悚","zh-tw":"驚悚","ky":"триллер","eu":"thriller","pl":"dreszczowiec","be":"трылер","he":"מותחן","fr":"thriller","ko":"스릴러","es":"suspenso","af":"thriller","hu":"thriller","it":"thriller","gl":"suspense","et":"triller","id":"cerita seru","de":"Thriller","ja":"スリラー","nl":"thriller","ar":"إثارة","sv":"thriller","pt":"thriller","eo":"trilero","sk":"triler","ru":"триллер","hy":"թրիլեր","en":"thriller","tr":"gerilim","ro":"thriller","ca":"thriller","fi":"trilleri","uk":"трилер","la":"Thriller","be-tarask":"трылер","fo":"Nøtrisøga","cs":"thriller","fa":"دلهره‌آور","bg":"трилър","az":"triller","hr":"triler","lt":"trileris","da":"thriller","an":"thriller","zh":"驚悚","sr":"трилер","sr-ec":"трилер","sr-el":"triler","nb":"thriller","mk":"трилер","sco":"thriller","lv":"trilleris","tg":"триллер","fy":"Skriller","ka":"თრილერი","sh":"triler","el":"θρίλερ","ckb":"چەشنی ھەستبزوێن","bn":"রোমাঞ্চকর সৃষ্টিকর্ম","lb":"Thriller","ur":"سنسنی خیز","cy":"cyffro","tt":"триллер","tt-cyrl":"триллер","bho":"थ्रिलर","vi":"kịch tính","th":"แนวระทึกขวัญ","ms":"genre debaran","sq":"trillim","ga":"scéinséir","ast":"suspense","myv":"триллер","hyw":"Զգայացունց տրամա","wuu":"惊悚","uz":"triller","yue":"驚慄","xmf":"თრილერი","pt-br":"suspense","nn":"thriller","en-gb":"thriller","hi":"थ्रिलर (शैली)","sl":"triler","km":"តក់ស្លុត","ce":"триллер","sd":"سنسني خيز","mzn":"تریلر"},"aliases":{"zh":["驚悚作品"],"he":["סרט מתח","מותחן פסיכולוגי","מתח","מותחן פשע"],"fr":["Thriller (littérature)"],"ko":["드릴러","스릴러 영화"],"es":["thriller","suspense","intriga"],"id":["tegang","getaran"],"nl":["literaire thriller"],"ar":["إثاره"],"sv":["triller","thrillern","rysare"],"pt":["suspense"],"eo":["thriller","suspensfilmo","suspensa filmo"],"ru":["триллеры"],"ca":["suspens"],"uk":["Трілер","Триллер","трилери"],"be-tarask":["трымценьнік"],"cs":["Triller","Psychothriller"],"fa":["فیلم دلهره‌آور","دلهره‌اور","فیلم هیجانی","دلهره اور","دلهرهٔ آور","دلهره آور"],"hr":["Thriller"],"pl":["thriller"],"en":["suspense","suspense fiction","thriller fiction","suspense thriller"],"th":["แนวตื่นเต้น","แนวเขย่าขวัญ"],"tr":["endişe"],"ms":["Filem seram ngeri"],"mk":["трилер (жанр)"],"uz":["triller (janr)"],"bn":["থ্রিলার"],"pt-br":["thriller"],"en-gb":["suspense","thriller fiction","suspense fiction"],"nb":["thrillersjangeren"],"km":["ភាពយន្តតក់ស្លុត","ព្រឺព្រួច","អាថ៌កំបាំងបែបរន្ធត់","ស៊ើបអង្កេតបែបរន្ធត់","រន្ធត់","ភិតភ័យ"],"mzn":["دل‌توآر","دل تو آر","دلتویار","زهله یار","زهله آر"]},"descriptions":{"fr":"genre artistique","en":"genre of fiction","nl":"genre","de":"Literatur- und Filmgenre","it":"genere letterario, cinematografico e televisivo","ru":"жанр художественного произведения","pl":"gatunek filmu","pt":"gênero artístico","es":"género literario, cinematográfico y televisivo","sv":"genre i litteratur, film och TV-serier","ro":"gen de literatură, film, televiziune","cs":"žánr filmového, televizního nebo literárního díla","ca":"gènere literari, cinematogràfic i televisiu","bn":"সাহিত্য, চলচ্চিত্র ও টেলিভিশন অনুষ্ঠানের প্রকার","uk":"особливий жанр кіно та літератури, в яких специфічні засоби повинні викликати у глядачів або читачів почуття тривожного очікування, невизначеності, хвилювання чи страху","sk":"žáner literárneho, filmového alebo televízneho diela","nb":"sjangerbetegnelse innen litteratur, drama, film og TV","zh":"文學，電影和電視節目類型","lv":"literatūras, kino un televīzijas žanrs","sq":"zhanër i letërsisë, filmave dhe programeve televizive","fi":"kirjallisuuden tai elokuvataiteen tyylilaji","id":"genre sastra, film, dan program televisi","tr":"edebiyat, film ve televizyon programcılığında bir tür","el":"είδος έργου λογοτεχνικού, κινηματογράφικού και τηλεοπτικού","ckb":"ژانری ئەدەبی و هونەری","he":"סוגה המיושמת בספרות, בקולנוע ובטלוויזיה","mk":"жанр","uz":"janr","cy":"genre mewn ffuglen","sr":"филмски, телевизијски и књижевни жанр","pt-br":"gênero de ficção","en-gb":"genre of fiction","eo":"ĝenro de literaturo, komputilaj ludoj kaj filmoj, kiu uzas suspenson, tension kaj psikan eksciton kiel stilaj elementoj","mzn":"اتی ژانر","da":"genre indenfor film, litteratur m.v.","be-tarask":"мастацкі жанр"},"sitelinks":{"afwiki":{"title":"Riller","badges":[]},"arwiki":{"title":"إثارة (نوع فني)","badges":[]},"azwiki":{"title":"Triller (janr)","badges":[]},"be_x_oldwiki":{"title":"Трылер","badges":[]},"bewiki":{"title":"Трылер","badges":[]},"bgwiki":{"title":"Трилър","badges":[]},"bhwiki":{"title":"थ्रिलर","badges":[]},"bnwiki":{"title":"রোমাঞ্চকর সৃষ্টিকর্ম","badges":[]},"cawiki":{"title":"Thriller","badges":[]},"cewiki":{"title":"Триллер","badges":[]},"ckbwiki":{"title":"چەشنی ھەستبزوێن","badges":[]},"commonswiki":{"title":"Category:Thriller","badges":[]},"cswiki":{"title":"Thriller","badges":[]},"dawiki":{"title":"Thriller","badges":[]},"dewiki":{"title":"Thriller","badges":[]},"enwiki":{"title":"Thriller (genre)","badges":[]},"eowiki":{"title":"Trilero","badges":[]},"eswiki":{"title":"Suspenso","badges":[]},"etwiki":{"title":"Triller (žanr)","badges":[]},"euwiki":{"title":"Thriller","badges":[]},"fawiki":{"title":"دلهره‌آور (ژانر)","badges":[]},"fiwiki":{"title":"Trilleri","badges":[]},"fowiki":{"title":"Nøtrisøga","badges":[]},"frwiki":{"title":"Thriller (genre)","badges":[]},"fywiki":{"title":"Skriller","badges":[]},"glwiki":{"title":"Suspense","badges":[]},"hewiki":{"title":"מותחן","badges":[]},"hiwiki":{"title":"थ्रिलर (शैली)","badges":[]},"hrwiki":{"title":"Triler","badges":[]},"huwiki":{"title":"Thriller (műfaj)","badges":[]},"hywiki":{"title":"Թրիլլեր","badges":[]},"idwiki":{"title":"Cerita seru","badges":[]},"itwiki":{"title":"Thriller","badges":[]},"jawiki":{"title":"スリラー","badges":[]},"kawiki":{"title":"თრილერი","badges":[]},"kowiki":{"title":"스릴러","badges":[]},"kywiki":{"title":"Триллер","badges":[]},"lawiki":{"title":"Fabula formidulosa","badges":[]},"lbwiki":{"title":"Thriller","badges":[]},"ltwiki":{"title":"Trileris","badges":[]},"lvwiki":{"title":"Trilleris","badges":[]},"mkwiki":{"title":"Трилер (жанр)","badges":[]},"mswiki":{"title":"Genre debaran","badges":[]},"myvwiki":{"title":"Триллер","badges":[]},"nlwiki":{"title":"Thriller (genre)","badges":[]},"nowiki":{"title":"Thriller","badges":[]},"plwiki":{"title":"Dreszczowiec","badges":[]},"ptwiki":{"title":"Thriller (gênero)","badges":[]},"rowiki":{"title":"Thriller (gen)","badges":[]},"ruwiki":{"title":"Триллер","badges":[]},"ruwikinews":{"title":"Категория:Триллер","badges":[]},"sdwiki":{"title":"سنسني خيز","badges":[]},"shwiki":{"title":"Triler (žanr)","badges":[]},"simplewiki":{"title":"Thriller (genre)","badges":[]},"skwiki":{"title":"Triler","badges":[]},"srwiki":{"title":"Triler (žanr)","badges":[]},"svwiki":{"title":"Thriller","badges":[]},"tgwiki":{"title":"Триллер","badges":[]},"thwiki":{"title":"แนวระทึกขวัญ","badges":[]},"trwiki":{"title":"Gerilim (tür)","badges":[]},"ukwiki":{"title":"Трилер","badges":[]},"ukwikiquote":{"title":"Трилер","badges":[]},"urwiki":{"title":"سنسنی خیز (صنف)","badges":[]},"uzwiki":{"title":"Triller (janr)","badges":[]},"viwiki":{"title":"Giật gân (thể loại)","badges":[]},"wuuwiki":{"title":"惊悚","badges":[]},"xmfwiki":{"title":"თრილერი","badges":[]},"zh_yuewiki":{"title":"驚慄","badges":[]},"zhwiki":{"title":"驚悚","badges":[]}},"claims":{"wdt:P31":["wd:Q108465955"],"wdt:P227":["4185351-9"],"wdt:P18":["North by Northwest movie trailer screenshot (37).jpg"]},"lastrevid":2241285782,"image":{"url":"https://commons.wikimedia.org/wiki/Special:FilePath/North%20by%20Northwest%20movie%20trailer%20screenshot%20%2837%29.jpg?width=1000","file":"North by Northwest movie trailer screenshot (37).jpg","credits":{"text":"Wikimedia Commons","url":"https://commons.wikimedia.org/wiki/File:North by Northwest movie trailer screenshot (37).jpg"}}}},"redirects":{}} 
            """,
        )
        val genre2 = MockRestResponseCreators.withSuccess().body(
            """
                {"entities":{"wd:Q5937792":{"uri":"wd:Q5937792","wdId":"Q5937792","labels":{"fa":"داستان‌های جنایی","nl":"krimi","de":"Krimi","es":"género policíaco","en":"crime fiction","eo":"krimfikcio","hr":"Žanr kriminalistike","ca":"gènere policíac","ar":"أدب الجريمة","bs":"Kriminalistički žanr","ru":"криминальный жанр","fr":"genre policier","de-ch":"Krimi","en-ca":"Crime fiction","en-gb":"Crime fiction","sh":"Kriminalistički žanr","ta":"குற்றப்புனைவு","tr":"Polisiye","zh":"犯罪作品","af":"misdaadfiksie","it":"giallo","sv":"deckare","ro":"ficțiune polițistă","hu":"krimi","mk":"криминалистички жанр","fi":"rikoskirjallisuus","el":"αστυνομική μυθοπλασία","ko":"범죄물","cs":"kriminální fikce","ja":"犯罪フィクション","sr":"kriminalistička fantastika","da":"krimi","bn":"অপরাধ কল্পকাহিনী","hy":"քրեական","nb":"krim","he":"ספרות פשע","ms":"Jenayah","id":"fiksi kriminal","vi":"truyện trinh thám","be-tarask":"крымінальны жанр","fy":"Misdiefiksje","yue":"犯罪故仔","sd":"ڪرائيم فڪشن","az":"cinayət janrı","th":"นิยายสืบสวนสอบสวน","uk":"кримінальний жанр","nn":"krim","lv":"Kriminālliteratūra","bg":"Криминална литература","is":"Glæpasaga","zh-hans":"犯罪作品","zh-hant":"虛構犯罪","cy":"ffuglen drosedd","sms":"kriminaalǩeerjlažvuõtt","ast":"xéneru policiacu","pl":"fikcja kryminalna","pt":"Ficção policial","ga":"ficsean coiriúlachta","ur":"جرم فکشن","ckb":"وێژەی تاوانباری","se":"rihkusgirjjálašvuohta","smn":"rikoskirjálâšvuotâ","sl":"kriminalna fikcija","os":"криминалон жанр","lb":"Krimi","lmo":"sgiald"},"aliases":{"fa":["ادبیات داستانی جنایی"],"es":["género policial","policial","policiaco","policíaco","ficción criminal","ficción policíaca","genero policiaco","genero policial","ficcion criminal","ficcion policiaca"],"hu":["bűnügyi történet"],"ko":["크라임 픽션"],"cs":["detektivní fikce"],"ja":["クライムフィクション","クライムスリラー","犯罪小説","殺人ミステリー","ディテクティブ・ストーリー","探偵物語","ディテクティブストーリー"],"sr":["kriminalistička drama"],"zh-hans":["犯罪小说"],"ru":["криминальный вымысел","полицейское произведение","криминал"],"zh":["終極警網","兇殺懸疑"],"sv":["kriminalfiktion"],"cy":["stori dditectif","dirgelwch llofruddiaeth"],"nn":["kriminallitteratur","krimlitteratur"],"fi":["rikosfiktio"],"ast":["policiacu"],"ca":["gènere de ficció criminal"],"ur":["کرائم فکشن"],"el":["αστυνομικό δράμα"],"sl":["kriminalno leposlovje","kriminalka"]},"descriptions":{"en":"genre of fiction focusing on crime, encompassing literature, film and theatre","es":"género de obras creativas con un enfoque en crímenes","ru":"художественный жанр","it":"genere letterario, televisivo e cinematografico","ko":"범죄에 초점이 맞춰져 있는 장르의 한 종류","cs":"žánr fikce zaměřený na zločin","nb":"sjanger innen fiksjon med fokus på kriminalitet","de":"Genre","eo":"ĝenro de fikcio kies ĉefa temo estas krimo kaj investigado, inkluzivigas literaturon, filmon kaj teatron","fr":"catégorie de fictions sur le thème de la criminalité","ca":"gènere de ficció centrat en el crim","sr":"žanr fikcije koji se fokusira na zločin","be-tarask":"мастацкі жанр","sv":"genre inom fiktion vars huvudtema är kriminalitet och brottsutredning, omfattar literatur, film och teater","da":"fiktionsgenre","tr":"Suç ve suçlularla ilgili kurgu","uk":"художній жанр","zh-hans":"文类","zh":"文类","fi":"kirjallisuuden lajityyppi","id":"genre fiksi yang berfokus pada tindakan kriminal","nl":"Duitstalige benaming van het misdaadgenre","cy":"ffuglen sy'n canolbwyntio ar drosedd","ja":"探偵フィクションを含むフィクションのジャンル","vi":"thể loại giả tưởng tập trung vào tội phạm, bao gồm văn học, phim ảnh và sân khấu","lmo":"sgender literari"},"sitelinks":{"arwiki":{"title":"أدب الجريمة","badges":[]},"bnwiki":{"title":"অপরাধ কল্পকাহিনী","badges":[]},"bswiki":{"title":"Kriminalistički žanr","badges":[]},"cawiki":{"title":"Gènere policíac","badges":[]},"ckbwiki":{"title":"وێژەی تاوانباری","badges":[]},"dewiki":{"title":"Krimi","badges":[]},"elwiki":{"title":"Αστυνομικό μυθιστόρημα","badges":[]},"enwiki":{"title":"Crime fiction","badges":[]},"eowiki":{"title":"Krimfikcio","badges":[]},"eswiki":{"title":"Ficción criminal","badges":[]},"fawiki":{"title":"داستان جنایی","badges":[]},"fiwiki":{"title":"Rikoskirjallisuus","badges":[]},"frwiki":{"title":"Genre policier","badges":[]},"fywiki":{"title":"Misdiefiksje","badges":[]},"hrwiki":{"title":"Žanr kriminalistike","badges":[]},"hywiki":{"title":"Քրեական գեղարվեստական գրականություն","badges":[]},"idwiki":{"title":"Fiksi kejahatan","badges":[]},"iswiki":{"title":"Glæpasaga","badges":[]},"itwiki":{"title":"Giallo (genere)","badges":[]},"itwikiquote":{"title":"Letteratura gialla","badges":[]},"jawiki":{"title":"犯罪小説","badges":[]},"kowiki":{"title":"범죄물","badges":[]},"lmowiki":{"title":"Romanz sgiald","badges":[]},"mswiki":{"title":"Cereka jenayah","badges":[]},"nlwiki":{"title":"Krimi","badges":[]},"ptwiki":{"title":"Ficção policial","badges":[]},"sdwiki":{"title":"ڪرائيم فڪشن","badges":[]},"shwiki":{"title":"Kriminalistički žanr","badges":[]},"simplewiki":{"title":"Crime fiction","badges":[]},"srwiki":{"title":"Kriminalistička fantastika","badges":[]},"svwiki":{"title":"Deckare","badges":["Q17559452"]},"tawiki":{"title":"குற்றப்புனைவு","badges":[]},"trwiki":{"title":"Polisiye","badges":[]},"zh_yuewiki":{"title":"犯罪故仔","badges":[]},"zhwiki":{"title":"犯罪小說","badges":[]}},"claims":{"wdt:P31":["wd:Q108465955"],"wdt:P227":["4165727-5"],"wdt:P244":["sh85033995"],"wdt:P268":["12453012w"],"wdt:P921":["wd:Q83267"],"wdt:P1889":["wd:Q20664530","wd:Q208505","wd:Q1438652"]},"lastrevid":2241164757,"image":{}}},"redirects":{}}
            """,
        )
        val series = MockRestResponseCreators.withSuccess().body(
            """
               {"entities":{"wd:Q27536277":{"uri":"wd:Q27536277","wdId":"Q27536277","type":"serie","labels":{"fr":"Commissaire Jean-Baptiste Adamsberg","en":"Commissioner Jean-Baptiste Adamsberg","de":"Kommissar Jean-Baptiste Adamsberg","it":"Commissario Jean-Baptiste Adamsberg","ga":"Commissaire Jean-Baptiste Adamsberg"},"aliases":{},"descriptions":{"fr":"série de livre de Fred Vargas","en":"book serie by Fred Vargas","de":"Buchserie von Fred Vargas mit diesem Protagonisten","it":"serie di libri di Fred Vargas"},"sitelinks":{},"claims":{"wdt:P31":["wd:Q277759"],"wdt:P50":["wd:Q237087"],"wdt:P136":["wd:Q208505"],"wdt:P407":["wd:Q150"],"wdt:P1476":["Commissaire Jean-Baptiste Adamsberg"],"wdt:P674":["wd:Q1684606"]},"originalLang":"fr","lastrevid":1716997222,"image":{}}},"redirects":{}} 
            """,
        )

        serv.expect(MockRestRequestMatchers.requestTo("https://inventaire.io/api/entities?action=by-uris&uris=isbn:9782290349229")).andExpect(MockRestRequestMatchers.header(HttpHeaders.USER_AGENT, USER_AGENT + buildProperties.version)).andRespond(isbn)
        serv.expect(MockRestRequestMatchers.requestTo("https://inventaire.io/api/entities?action=by-uris&uris=wd:Q3203603")).andRespond(edition)
        serv.expect(MockRestRequestMatchers.requestTo("https://inventaire.io/api/entities?action=by-uris&uris=wd:Q237087")).andRespond(author)
        serv.expect(MockRestRequestMatchers.requestTo("https://inventaire.io/api/entities?action=by-uris&uris=wd:Q182015")).andRespond(genre1)
        serv.expect(MockRestRequestMatchers.requestTo("https://inventaire.io/api/entities?action=by-uris&uris=wd:Q5937792")).andRespond(genre2)
        serv.expect(MockRestRequestMatchers.requestTo("https://inventaire.io/api/entities?action=by-uris&uris=wd:Q27536277")).andRespond(series)
        val jeluProperties = JeluProperties(
            JeluProperties.Database(""),
            JeluProperties.Files("", "", true),
            JeluProperties.Session(1),
            JeluProperties.Cors(),
            JeluProperties.Metadata(JeluProperties.Calibre("")),
            JeluProperties.Auth(JeluProperties.Ldap(), JeluProperties.Proxy()),
            listOf(
                JeluProperties.MetaDataProvider(
                    "inventaireio",
                    true,
                    apiKey = null,
                ),
            ),
        )
        val service = InventaireIoMetadataProvider(builder.build(), jeluProperties, ObjectMapper(), buildProperties)

        // When
        val result: MetadataDto? = service.fetchMetadata(
            MetadataRequestDto("9782290349229"),
            mapOf(),
        )?.get()

        // Then
        Assertions.assertNotNull(result)
        Assertions.assertEquals("L'homme aux cercles bleus", result?.title)
        Assertions.assertEquals("2-290-34922-4", result?.isbn10)
        Assertions.assertEquals("978-2-290-34922-9", result?.isbn13)
        Assertions.assertEquals(mutableSetOf("Fred Vargas"), result?.authors)
        Assertions.assertEquals(
            "https://inventaire.io/img/entities/57883743aa7c6ad25885a63e6e94349ec4f71562",
            result?.image,
        )
        Assertions.assertEquals("fr", result?.language)
        Assertions.assertEquals("2005-05-01", result?.publishedDate)
        Assertions.assertEquals("1991 mystery novel by Fred Vargas, 1st of her Detective Adamsberg series", result?.summary)
    }

    @Test
    fun testParseClaims() {
        val jeluProperties = JeluProperties(
            JeluProperties.Database(""),
            JeluProperties.Files("", "", true),
            JeluProperties.Session(1),
            JeluProperties.Cors(),
            JeluProperties.Metadata(JeluProperties.Calibre("")),
            JeluProperties.Auth(JeluProperties.Ldap(), JeluProperties.Proxy()),
            listOf(
                JeluProperties.MetaDataProvider(
                    "inventaireio",
                    true,
                    apiKey = null,
                ),
            ),
        )
        val objectMapper = ObjectMapper()
        val service = InventaireIoMetadataProvider(springRestClient, jeluProperties, objectMapper, buildProperties)
        val res = """
            {
              "wdt:P31" : [ "wd:Q3331189" ],
              "wdt:P212" : [ "978-2-290-34922-9" ],
              "wdt:P957" : [ "2-290-34922-4" ],
              "wdt:P407" : [ "wd:Q150" ],
              "wdt:P1476" : [ "L'homme aux cercles bleus" ],
              "wdt:P629" : [ "wd:Q3203603" ],
              "wdt:P123" : [ "wd:Q3156592" ],
              "invp:P2" : [ "57883743aa7c6ad25885a63e6e94349ec4f71562" ],
              "wdt:P577" : [ "2005-05-01" ],
              "wdt:P1104" : [ 220 ],
              "wdt:P2969" : [ "1508217" ]
            }
        """
        val node = objectMapper.readTree(res)
        val dto = ParsingDto(MetadataDto(), "")
        service.parseClaims(node, dto)
        Assertions.assertEquals("978-2-290-34922-9", dto.metadataDto.isbn13)
        Assertions.assertEquals("2-290-34922-4", dto.metadataDto.isbn10)
        Assertions.assertEquals("2005-05-01", dto.metadataDto.publishedDate)
        Assertions.assertEquals(220, dto.metadataDto.pageCount)
        Assertions.assertEquals("1508217", dto.metadataDto.goodreadsId)
        Assertions.assertEquals("L'homme aux cercles bleus", dto.metadataDto.title)
        Assertions.assertEquals("wd:Q3203603", dto.editionClaim)
    }
}
