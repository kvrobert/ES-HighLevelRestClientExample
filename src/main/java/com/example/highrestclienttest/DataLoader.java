package com.example.highrestclienttest;

import com.example.highrestclienttest.beans.TestData;
import com.example.highrestclienttest.service.ElasticSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private ElasticSearchService elasticSearchService;

    List<TestData> datas;


    @Override
    public void run(String... args) throws Exception {
      //  datas = new ArrayList();
      //  initializeData();  // így nem hozza létre minden indításnál a Robeszt
    }

    private void initializeData() throws IOException {
        TestData robesz = TestData.builder()
                .name(Arrays.asList("Robesz", "Gabi"))
                .age("25")
                .hobby("developing")
                .allow_token_parent("Precognox:S-1-5-21-3014129096-3214889382-4178971525-1157")
                .build();

        TestData karesz = TestData.builder()
                .name(Arrays.asList("Kása Károly"))
                .age("45")
                .hobby("running")
                .allow_token_parent("TOKEN:Precognox:S-1-5-21-3014129096-3214889382-4178971525-513")
                .build();

        TestData adam = TestData.builder()
                .name(Arrays.asList("Kulcsár Ádám"))
                .age("28")
                .hobby("music")
                .allow_token_parent("TOKEN:Precognox:S-1-5-21-3014129096-3214889382-4178971525-1144")
                .build();

        TestData robi = TestData.builder()
                .name(Arrays.asList("Kiss-Vincze Róbert"))
                .age("37")
                .allow_token_parent("TOKEN:Precognox:S-1-5-21-3014129096-3214889382-4178971525-1146")
                .build();

        TestData tibi = TestData.builder()
                .name(Arrays.asList("Kovács Tibor"))
                .age("40")
                .hobby("Az önéletrajz hobbi rovatát - hasonlóan a CV többi részéhez - a naprakészség miatt folyamatosan frissíteni, aktualizálni kell. \nInkább ne is tegyünk említést a hobbiról, ha pillanatnyilag nem vagyunk naprakészek a témában, vagy egyszer, valamikor réges-régen űztük azt \"- tanácsolja Lovász Sándor, az Asio Személyzeti és Gazdaságpszichológiai Tanácsadó Iroda szakmai igazgatója.")
                .build();

        TestData kazmer = TestData.builder()
                .name(Arrays.asList("Vandál Kázmér"))
                .age("22")
                .hobby("Miután kiderült a témában teljes tájékozatlanságuk, végül egyikük sem nyerte el a kívánt pozíciót. Hiszen aki egy ilyen jelentéktelen\n témában is füllent, attól a napi munkavégzés során sem várható őszinteség, kiszámíthatóság \" - figyelmeztet a szakember. Összességében tehát elmondható: a CV szabadidő/hobbi-rovatának tartalma többet elárul rólunk, mintsem feltételeznénk. Ám ha nincsen hobbink, a kellemetlenségeket elkerülendő, inkább hagyjuk ki.  \n" +
                        "\n")
                .allow_token_parent("TOKEN:Precognox:S-1-5-21-3014129096-3214889382-4178971525-1140")
                .build();

        TestData jozsi = TestData.builder()
                .name(Arrays.asList("Molnár József"))
                .age("60")
                .hobby("A nem szellemi sportok említése, részletezése néhány címszóban szintén jelzésértékű: aki például a napi 9-10 órányi \n munka után is rendszeresen sportol, valószínűleg kitartó, nagyobb teljesítményre képes és hatékonyabban kezeli a stresszel járó helyzeteke. És ilyen szakembert mindenki szívesen alkalmaz\" - hívja fel a figyelmet Herczku Mónika, a Karrier Mentor Tanácsadó Iroda vezetője. Az extrém sport ugyancsak pozitívumnak számít az önéletrajzokban. Az ilyet űzőkről valószínűsíthető: jól tűrik a stresszt, merészek, szeretik az innovatív kezdeményezéseket és könnyedén megbirkóznak nem várt helyzetekkel is.")
                .allow_token_parent("TOKEN:Precognox:S-1-5-21-3014129096-3214889382-4178971525-1140")
                .build();

        TestData levente = TestData.builder()
                .name(Arrays.asList("Tóth Levente"))
                .age("29")
                .hobby("Hosszú időn át az volt az elfogadott, hogy az ember bizonyos intelligencia-szinttel születik, és akkor jár a legjobban, ha ehhez mérten lövi be a képességeit és lehetőségeit. A tudomány egy ideje viszont már azt mondja, hogy simán képesek vagyunk növelni a magunkban rejlő potenciált, hiszen új készségek elsajátításával\n az agy új idegpályákat teremt, amelyek gyorsabban és jobban működnek.")
                .allow_token_parent("TOKEN:Precognox:S-1-5-21-3014129096-3214889382-4178971525-1146")
                .build();

        TestData akos = TestData.builder()
                .name(Arrays.asList("Marosics Ákos"))
                .age("43")
                .hobby("\\nTERRORELHÁRÍTÁSI KÖZPONT \\n\\nBŰNÜGYI FŐOSZTÁLY \\n\\nFELDERÍTŐ OSZTÁLY \\n \\n \\nSzám: 52100-1-45/25/2013.tük. \\n  \\n \\n Minősítési szint: „Titkos!” \\n Érvényességi idő: 2044. év december hó 31. nap \\n Minősítő: Kiss Géza r. őrnagy, osztályvezető \\n\\nKülönleges kezelési utasítás: „Nem sokszorosítható!”, \\n„Különösen fontos!” \\n\\n 1. sz. pld. \\n \\n\\n \\n\\nAntal András r. alezredes úrnak, \\n\\nTerrorelhárítási Központ \\n\\nBűnügyi Főosztály Értékelő Oosztály \\n\\nvezetője \\n\\nH E L Y B E N \\n\\nTisztelt Osztályvezető Úr! \\n\\nHatóságunk a Rendőrségről szóló 1994. évi XXXIV. törvény (Rtv.) 7/E. § (3) bekezdése \\n\\nalapján – a terrorcselekmény elkövetésére irányuló törekvések megelőzése, felderítése és \\n\\nelhárítása érdekében „SALAKPÁLYA” fedőnéven titkos információgyűjtést folytatunk. \\n\\nAz eljárás során látókörünkbe került \\n \\nZombory-Fekete Andor /szül.: Fekete Andor, Mátészalka, 1968.03.11. an.: Zombory Erzsébet / \\n\\nMátészalka, Dankó Pista u. 42. és  \\n \\n\\nFülöp Anna Mária / szül.: Révész Anna Mária, Hajdúszoboszló, 1959. 11. 12.an.: Molnár Gizella / \\n\\nNyíregyháza, Aradi u. 55. szám alatti lakos. \\n \\nNevezett személyek köthetők a Magyar Várvédők nevű centralista szervezethez, ezért \\nkérem, hogy a szervezettel összefüggésben-, valamint életvitelükre, kapcsolatrendszerükre,  \\ninternetes kutatást végeztetni szíveskedjen.  \\n \\n\\nKérem, hogy a nevezett személyekről megállapított információk jelentés formájában \\n\\nfőosztályunk részére történő megküldése iránt intézkedni szíveskedjen. \\n\\n \\n\\nBudapest, 2013. január 26.   \\n\\n\\n\\n \\n\\n \\n\\n                                                                  Tisztelettel: \\n\\n \\n\\n                                              \\n\\n \\n\\nKiss Géza r. őrnagy \\n\\n            osztályvezető \\n\\n \\n\\n\\n\"")
                .allow_token_parent("TOKEN:Precognox:S-1-5-21-3014129096-3214889382-4178971525-1111")
                .build();

        datas.add(robesz);
        datas.add(karesz);
        datas.add(adam);
        datas.add(robi);
        datas.add(tibi);
        datas.add(kazmer);
        datas.add(jozsi);
        datas.add(levente);
        datas.add(akos);

       // elasticSearchService.index();
        // elasticSearchService.createRecord(robesz);

        datas.stream().forEach( data -> elasticSearchService.createRecord(data));
    }


}
