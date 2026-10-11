# MOTORE-RADIOFONICO
Web Radio Automatica (by Ciry) è un'app Android che trasforma lo smartphone in una regia radiofonica professionale. Offre mixing automatico multi-player senza pause, gestione playlist e cartelle, programmazione spot, notizie live via feed RSS di Sky TG24, VU meter a LED e modalità console protetta da PIN per l'uso H24.


=== MANUALE UTENTE: WEB RADIO AUTOMATICA (by Ciry) ===

Benvenuto nella documentazione ufficiale di Web Radio Automatica. Questa applicazione per Android è progettata per operare come una vera e propria regia radiofonica, miscelando automaticamente brani musicali, jingle, spot pubblicitari e notiziari live con qualità da studio.

-- 1. PRIMO AVVIO E PERMESSI --
Essendo un'app che si basa sui tuoi file audio personali (MP3) salvati sul dispositivo, al primo avvio la radio richiederà l'Accesso Completo ai File ("All Files Access" / "Permesso di Gestione Tutti i File").
Senza questo permesso, l'app non potrà scansionare le cartelle per caricare la musica. Una volta concesso, l'automazione partirà da sola ed elencherà tutta la musica strutturata. Sentirai i motori in azione appena premerai PLAY.
Inoltre, all'avvio visualizzerai la Splash Screen con il logo della radio visualizzato per 5 secondi precisi.

-- 2. L'INTERFACCIA PRINCIPALE E LA DASHBOARD --
Finito il caricamento, visualizzerai la "Dashboard", il pannello di controllo della console.
Quest'area e' strutturata per essere interamente scorrevole verso il basso.
Nella dashboard troverai:
- L'orologio digitale nativo e la Percentuale di Batteria: Costantemente aggiornati e visibili in grande. Ideale per tenere d'occhio il tablet o lo smartphone anche da lontano.
- Titolo in onda: L'indicatore a scorrimento monospazio fluo che mostrerà il titolo del file aperto.
- Pulsante Cestino: Situato accanto al titolo. Serve ad eliminare tempestivamente dal dispositivo il brano in onda se e' di cattivo gusto o rotto. L'app chiedera' sempre conferma fissa.
- Barra temporale: Mostra il minutaggio attuale del brano ed il suo tempo totale.
- VU Meter a LED: Le iconiche barre verticali animate che indicano il volume dell'uscita audio della regia diviso per scaglioni colorati (Verde in basso per frequenze sicure, Giallo a volume intenso, Rosso per i picchi caldi).

-- 3. GESTIONE DELLA RIPRODUZIONE (PLAYER & MIXING) --
L'app utilizza tre "Motori Audio" nativi in background (identificati dal codice come Player A, Player B e un terzo Player per le basi SFX) studiati per incrociarsi in sincronia perfetta, senza lasciare "buchi" di silenzio neri tra una canzone e l'altra.
Alla voce "CONTROLLI DIRETTA" trovi:
* PLAY/PAUSA (Verde): Avvia o sospende momentaneamente tutta la regia e il timer dei programmi in background.
* SKIP / MIX (Giallo): Se premuto, inizia istantaneamente l'assottigliamento di chiusura (sfumatura/fade) veloce dell'attuale brano fino a fonderlo col jingle in transizione o con il nuovo brano agganciato. Non "taglia in tronco" per non spezzare l'armonia d'ascolto. L'effetto e' fluido.

-- 4. LA SCELTA E GESTIONE DELLE PLAYLIST MUSICALI --
L'automazione sceglie i file dai cluster di archiviazione esterni del dispositivo. Per decidere che genere riprodurre, la sezione "SELEZIONE PLAYLIST" presenta diverse spunte:
- PLAY 1, PLAY 2... fino a PLAY 6: Permettono di vincolare la stazione a riprodurre musica prelevandola unicamente da una specifica cartella tematica.
- RANDOM PLAYLIST (1-5): La modalita' principe e piu' dinamica della radio. L'algoritmo andrà a scavare casualmente nelle prime 5 configurazioni di playlist/cartelle, garantendo varietà estrema al palinsesto e non ripetendo le identiche stazioni di frequente. Consigliata per riempimento passivo H24.

Oltre all'algoritmo automatico puro, puoi inserire "a mano" brani estemporanei esterni col tasto arancione "+ AGGIUNGI BRANO (FILE BROWSER)". Scegli e tocca un file MP3 per accodarlo con priorità. Questa canzone suonerà subito dopo la conclusione del brano/pubblicità o del mixing attuale. Finito il brano esplicito, l'app riprenderà la lista musicale e l'algoritmo di normalita' di prima. 

-- 5. AUTOMAZIONE EVENTI (SPOT/PUBBLICITÀ E NEWS/TELEGIORNALE) --
Una Web-Radio in onda si distingue grazie alla cadenza precisa degli inserti parlati. E' per questo che l'app integra 2 automazioni passive continue:

* Gestione Spot Commerciali (SPOT ON)
Accendendo l'interrutore, puoi specificare nel box di testo da quanti brani dev'essere distanziata ogni comparsa della cartella "Spot". Inserendo "Ogni 5 brani", il sistema si incarica di richiamare in miscelazione automatica il Player dedicato agli spot passate cinque distinte canzoni, per poi incatenare di nuovo i jingles di ritorno prima di ricominciare il palinsesto musicale. 

* Timer Informativi e Redazione Giornalistica (NEWS ON)
Spostando il rispettivo "Switch On" azzurro per le notizie, puoi digitare il timer "Ogni [ X ] minuti" (es. 60 minuti se vuoi il radiogiornale ogni ora precisa). 
Funzionamento invisibile: Completamente in autonomia di background, l'app preleverà via internet i feed RSS di "Sky TG24" dal server centrale Spreaker Italia. Immediato l'avvenuto scaricamento e tracciamento dell'URL mp3 aggiornato all'ultima ora, inserira' fluidamente l'audio in testa alla scaletta garantendo un aggancio perfetto ("in battuta"). Attenderà la fine del brano correntemente in ascolto prima di avviarlo, in modo che l'estratto parlato di durata non trascurabile non subisca sovrapposizioni, ne' tagli o dissolvenze di chiusura che interromperebbero prematuramente il giornalista.

-- 6. IMPOSTAZIONI E SICUREZZA ("Console Bloccata") --
In alto a destra troverai il simbolo di un Lucchetto. Finche' rimane "Chiuso/Rosso", l'operatore non puo' toccare interruttori delicati, spegnere la riproduzione, eliminare fisicamente un brano dal cestino e nemmeno aprire la barra configurazioni.
Cliccalo e inserisci la password nativa (di fabbrica: 1234) per scatenare le funzionalita' amministrative e il lucchetto "Verde/Sbloccato".

* Cliccando sul Simbolo della CHIAVE INGLESE, accedi alle impostazioni nascoste per:
1. Dichiarare ed imporre un PIN differente per il login;
2. Configurare letteralmente il "nome esatto delle cartelle" preinstallate nel tablet dentro le quali devi inserire le tracce (per esempio, decidere che la cartella "Music1" nella vera memoria si chiama in realta' "SuccessiAnni90", mentre la cartella Spot si chiami "ClientiRadio"), senza queste direttive logiche ai percorsi, lo scheduler si accorgerea' di "Cartelle Vuote". (Ricordati di premere "SALVA E TORNA").

-- 7. CHIUSURA COMPLETA DELLA RADIO --
Nota bene: quest'applicazione ha "due facce", la regia visiva (UI) e la regia reale (RadioPlayerService) che opera imperterrita. L'app continuera' a far scorrere il live 24h su 24h in stand-by finche' lo chiedi. Per estirpare il programma chiuderlo definitivamente spegnendo i server, usa esclusivamente la "freccia indietro/bottone indetro" del tuo Android. Si avviera' la ghiera sicura:
- Clicca: "SPEGNI RADIO" dal pop-up di Arresto.
