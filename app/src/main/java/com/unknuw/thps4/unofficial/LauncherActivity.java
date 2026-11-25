package com.unknuw.thps4.unofficial;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// The pre-game launcher, modelled on the sister ports' front ends: before the
// game starts, the player sees the game folder's state, manages the mod packs
// (order, switches, import, a mod.ini's name/version/author/description and
// cover picture) and the custom soundtrack (the .wma songs in THPS4/Music,
// each with its size and duration, and the limits stated), then launches.
// Everything the player decides here is written into the same settings.toml
// the game itself reads at boot (mods_order / mods_disabled / music_disabled
// / language), so the native side needs no other channel.
public class LauncherActivity extends Activity {

    private static final int BG = 0xFF0C141C;
    private static final int CARD = 0xFF16222E;
    private static final int CARD2 = 0xFF1E2E3F;
    private static final int ACCENT = 0xFFF7C948;
    private static final int TEXT = 0xFFF2F5F7;
    private static final int MUTED = 0xFF9DB2C3;
    private static final int GOOD = 0xFF7BD88F;
    private static final int BAD = 0xFFFF8585;

    private static final String SHARED = "/storage/emulated/0/THPS4";
    private static final String LOOSE = "<loose>";
    private static final int REQ_ZIP = 42;
    private static final int REQ_ISO = 43;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private LinearLayout content;
    private final Button[] navButtons = new Button[4];
    private int tab = 0;  // 0 home, 1 mods, 2 music, 3 about

    private File settingsFile;
    private final Map<String, String> settings = new HashMap<>();
    private int uiLang;  // the launcher UI follows the game's language choice

    // Every launcher string goes through here, in the language picked
    // on Home: English and Bahasa Indonesia ride in the calls
    // themselves, Spanish and Portuguese in the tables below (section
    // 146) - the panel and the game already speak all four.
    private String L(String en, String idn) {
        if (uiLang == 1) return idn;
        if (uiLang == 2) return Es(en);
        if (uiLang == 3) return Pt(en);
        return en;
    }

    private static String Es(String en) {
        switch (en) {
            case "Home": return "Inicio";
            case "Language": return "Idioma";
            case "\u25b6  Game": return "\u25b6  Juego";
            case "Music": return "M\u00fasica";
            case "About": return "Acerca de";
            case "Loading\u2026": return "Cargando\u2026";
            case "Storage access needed": return "Se necesita acceso al almacenamiento";
            case "The game, its mods and your music live in the THPS4 folder on the phone's storage. Android asks for all-files access once - allow it, then come back here.": return "El juego, sus mods y tu m\u00fasica est\u00e1n en la carpeta THPS4 del almacenamiento del tel\u00e9fono. Android pide acceso a todos los archivos una sola vez: perm\u00edtelo y vuelve aqu\u00ed.";
            case "Grant storage access": return "Permitir acceso";
            case "Game folder: ": return "Carpeta del juego: ";
            case "Status: looks valid (found the game).": return "Estado: v\u00e1lido (juego encontrado).";
            case "\u2b07  Import ISO": return "\u2b07  Importar ISO";
            case "Status: game not found.": return "Estado: juego no encontrado.";
            case "Put the extracted game (default.xbe with its data folder) or the game ISO in the THPS4 folder above, or in its assets subfolder. An SD card or USB stick works too: a THPS4 folder at its root.": return "Pon el juego extra\u00eddo (default.xbe con su carpeta data) o la ISO del juego en la carpeta THPS4 de arriba, o en su subcarpeta assets. Tambi\u00e9n sirve una tarjeta SD o una memoria USB: una carpeta THPS4 en su ra\u00edz.";
            case "The app moves the ISO into the THPS4 folder for you (the folder is created if needed).": return "La app mueve la ISO a la carpeta THPS4 por ti (la carpeta se crea si hace falta).";
            case "The game's own texts, menus and dialogue. Applies when the game starts.": return "Los textos, men\u00fas y di\u00e1logos del propio juego. Se aplica al iniciar el juego.";
            case "Mods: \u2026": return "Mods: \u2026";
            case "Tap to manage mod packs.": return "Toca para gestionar los packs de mods.";
            case "Custom tracks: \u2026": return "Temas personalizados: \u2026";
            case "Tap to manage your soundtrack.": return "Toca para gestionar tu banda sonora.";
            case "Mods on: ": return "Mods activos: ";
            case " of ": return " de ";
            case "Custom tracks: ": return "Temas personalizados: ";
            case " songs in ": return " temas en ";
            case " albums (": return " \u00e1lbumes (";
            case "\u25b6  Launch Game": return "\u25b6  Iniciar juego";
            case "Graphics, controls and the full settings live inside the game: Options \u2192 Port Settings, and the settings panel on L3+R3.": return "Los gr\u00e1ficos, los controles y todos los ajustes est\u00e1n dentro del juego: Opciones \u2192 Ajustes Port, y el panel de ajustes con L3+R3.";
            case "About this port": return "Sobre este port";
            case "An unofficial Android port of Tony Hawk's Pro Skater 4 (Xbox), recompiled for your phone. The game is not included \u2014 this port reads your original game data or ISO file from the THPS4 folder.": return "Un port no oficial de Tony Hawk's Pro Skater 4 (Xbox) para Android, recompilado para tu tel\u00e9fono. El juego no est\u00e1 incluido \u2014 este port lee los datos originales de tu juego o tu archivo ISO desde la carpeta THPS4.";
            case "Version ": return "Versi\u00f3n ";
            case "Support via Sociabuzz": return "Apoya v\u00eda Sociabuzz";
            case "Support via Saweria": return "Apoya v\u00eda Saweria";
            case "Updates": return "Actualizaciones";
            case "New builds are published on the project's GitHub Releases. Every build is signed with the same key, so an update installs over this version.": return "Las nuevas versiones se publican en GitHub Releases del proyecto. Todas llevan la misma firma, as\u00ed que una actualizaci\u00f3n se instala sobre esta versi\u00f3n.";
            case "Checking for updates\u2026": return "Buscando actualizaciones\u2026";
            case "Update available: THPS4 ": return "Actualizaci\u00f3n disponible: THPS4 ";
            case "You're on the latest version.": return "Ya tienes la \u00faltima versi\u00f3n.";
            case "No release found yet.": return "A\u00fan no hay ninguna versi\u00f3n publicada.";
            case "Check failed - no connection.": return "La b\u00fasqueda fall\u00f3: sin conexi\u00f3n.";
            case "Not checked yet.": return "A\u00fan no se ha buscado.";
            case "never": return "nunca";
            case "Last check: ": return "\u00daltima b\u00fasqueda: ";
            case "Check for updates": return "Buscar actualizaciones";
            case "Check automatically": return "Buscar autom\u00e1ticamente";
            case "When the launcher opens": return "Al abrir el launcher";
            case "Open Releases page": return "Abrir la p\u00e1gina de Releases";
            case "Cancel": return "Cancelar";
            case "Open THPS4 ": return "Abrir THPS4 ";
            case "Download again": return "Descargar de nuevo";
            case "Download ": return "Descargar ";
            case "Download incomplete - try again.": return "Descarga incompleta: int\u00e9ntalo de nuevo.";
            case "Download failed - check your connection.": return "La descarga fall\u00f3: revisa tu conexi\u00f3n.";
            case "Could not open the file.": return "No se pudo abrir el archivo.";
            case "Loose files": return "Archivos sueltos";
            case "Order and switches": return "Orden y activaci\u00f3n";
            case "Switched-on mods load when the game starts. The higher a mod is in the list, the more it counts: where two mods change the same file, the upper one wins.": return "Los mods activados se cargan al iniciar el juego. Cuanto m\u00e1s arriba est\u00e9 un mod en la lista, m\u00e1s cuenta: si dos mods cambian el mismo archivo, gana el de arriba.";
            case "A save made while playing a mod skater only loads while that mod is on. To remove a mod safely: switch to a stock skater in the game, save, then switch the mod off.": return "Un guardado hecho con un skater de mod solo carga mientras ese mod est\u00e9 activo. Para quitar un mod sin riesgo: cambia a un skater normal en el juego, guarda y luego desactiva el mod.";
            case "Mods folder: ": return "Carpeta de mods: ";
            case "\u2b07  Import mod (.zip)": return "\u2b07  Importar mod (.zip)";
            case "No file picker available": return "No hay selector de archivos";
            case "The game was not found yet, so its Mods folder is unknown. Put the game in place first (see Home).": return "El juego a\u00fan no se encontr\u00f3, as\u00ed que su carpeta Mods no se conoce. Primero pon el juego en su sitio (mira Inicio).";
            case "Loading mods\u2026": return "Cargando mods\u2026";
            case "No mods yet. A mod is a folder here with a data folder (files that replace the game's), a skater.ini (a character pack), textures in a Textures folder, or a mod.ini describing it. Import a .zip to begin.": return "A\u00fan no hay mods. Un mod es una carpeta aqu\u00ed con una carpeta data (archivos que reemplazan los del juego), un skater.ini (un pack de personaje), texturas en una carpeta Textures, o un mod.ini que lo describe. Importa un .zip para empezar.";
            case "\u2713  The switched-on mods work together.": return "\u2713  Los mods activados funcionan juntos.";
            case "by ": return "por ";
            case " files": return " archivos";
            case "on": return "activado";
            case "off": return "desactivado";
            case "Author ": return "Autor ";
            case " files\n": return " archivos\n";
            case "Folder: ": return "Carpeta: ";
            case "Close": return "Cerrar";
            case "Open page": return "Abrir p\u00e1gina";
            case "Delete this mod?": return "\u00bfEliminar este mod?";
            case "\n\nThe folder and everything in it is deleted from the Mods folder. This cannot be undone.": return "\n\nLa carpeta y todo su contenido se eliminan de la carpeta Mods. No se puede deshacer.";
            case "Delete": return "Eliminar";
            case "Looking for ISO files\u2026": return "Buscando archivos ISO\u2026";
            case "Browse for a file\u2026": return "Buscar un archivo\u2026";
            case "Move which ISO into the THPS4 folder?": return "\u00bfQu\u00e9 ISO quieres mover a la carpeta THPS4?";
            case "That is not an .iso file": return "Eso no es un archivo .iso";
            case "That ISO is already in the THPS4 folder": return "Esa ISO ya est\u00e1 en la carpeta THPS4";
            case "Replace the existing ISO?": return "\u00bfReemplazar la ISO actual?";
            case "Replace": return "Reemplazar";
            case "Moving ISO\u2026": return "Moviendo la ISO\u2026";
            case "Import ISO": return "Importar ISO";
            case "Moving ISO\u2026 ": return "Moviendo la ISO\u2026 ";
            case "ISO moved - the game is ready": return "ISO movida: el juego est\u00e1 listo";
            case "ISO copied, but the original could not be deleted - you can delete it yourself": return "ISO copiada, pero el original no se pudo eliminar: puedes borrarlo t\u00fa";
            case "Import failed": return "La importaci\u00f3n fall\u00f3";
            case "Game not found - no Mods folder yet": return "Juego no encontrado: a\u00fan no hay carpeta Mods";
            case "Importing\u2026": return "Importando\u2026";
            case "Imported ": return "Importado: ";
            case "Import failed (not a valid zip?)": return "La importaci\u00f3n fall\u00f3 (\u00bfno es un zip v\u00e1lido?)";
            case "Custom Soundtrack": return "Banda sonora personalizada";
            case "Music source": return "Fuente de m\u00fasica";
            case "The game's soundtrack, or your own songs played directly by the port. Applies when the game starts.": return "La banda sonora del juego, o tus propias canciones reproducidas directamente por el port. Se aplica al iniciar el juego.";
            case "Game soundtrack": return "Banda sonora del juego";
            case "My Music": return "Mi m\u00fasica";
            case "Custom tracks": return "Temas personalizados";
            case "Your songs are played by the port itself, straight from the Music folder - no conversion. The game's own music steps aside automatically, and the volume follows the game's Music Volume slider. Loose songs form one album; each subfolder is an album of its own. Switched-off songs are skipped.": return "El port reproduce tus canciones directamente desde la carpeta Music, sin conversi\u00f3n. La m\u00fasica del juego se aparta autom\u00e1ticamente y el volumen sigue el control Music Volume del juego. Las canciones sueltas forman un \u00e1lbum; cada subcarpeta es un \u00e1lbum propio. Las canciones desactivadas se saltan.";
            case "Your songs play inside the game as a custom soundtrack: Sound Options \u2192 Soundtrack. Loose songs in the Music folder form one album; each subfolder is an album of its own, named after the folder. Switched-off songs are skipped when the game builds the soundtrack at start.": return "Tus canciones suenan dentro del juego como banda sonora personalizada: Sound Options \u2192 Soundtrack. Las canciones sueltas en la carpeta Music forman un \u00e1lbum; cada subcarpeta es un \u00e1lbum propio, con el nombre de la carpeta. Las canciones desactivadas se saltan cuando el juego arma la banda sonora al iniciar.";
            case "Music folder: ": return "Carpeta de m\u00fasica: ";
            case "Limits": return "L\u00edmites";
            case "\u2022  Formats: MP3, M4A/AAC, OGG, Opus, FLAC, WAV - whatever the phone itself can play (WMA stays with the game soundtrack source)": return "\u2022  Formatos: MP3, M4A/AAC, OGG, Opus, FLAC, WAV: lo que el propio tel\u00e9fono pueda reproducir (WMA queda para la banda sonora del juego)";
            case "\u2022  No per-song size or duration limit": return "\u2022  Sin l\u00edmite de tama\u00f1o ni duraci\u00f3n por tema";
            case "\u2022  Format: WMA only - the game's own decoder plays them (44.1 kHz stereo is what the original Xbox dashboard used)": return "\u2022  Formato: solo WMA: los reproduce el decodificador del propio juego (44,1 kHz est\u00e9reo es lo que usaba el dashboard original de Xbox)";
            case "\u2022  Max size: 64 MB per song": return "\u2022  Tama\u00f1o m\u00e1ximo: 64 MB por tema";
            case "\u2022  Max amount: 500 songs per album, 100 albums": return "\u2022  Cantidad m\u00e1xima: 500 temas por \u00e1lbum, 100 \u00e1lbumes";
            case "\u2022  Duration: read from each song file - there is no per-song duration limit": return "\u2022  Duraci\u00f3n: se lee de cada archivo: no hay l\u00edmite de duraci\u00f3n por tema";
            case "Loading songs\u2026": return "Cargando temas\u2026";
            case "No songs yet. Copy MP3 (or M4A, OGG, FLAC, WAV) files into the Music folder above and they will show up here.": return "A\u00fan no hay temas. Copia archivos MP3 (o M4A, OGG, FLAC, WAV) en la carpeta Music de arriba y aparecer\u00e1n aqu\u00ed.";
            case "No songs yet. Copy .wma files into the Music folder above and they will show up here.": return "A\u00fan no hay temas. Copia archivos .wma en la carpeta Music de arriba y aparecer\u00e1n aqu\u00ed.";
            case " songs \u00b7 ": return " temas \u00b7 ";
            case "  \u00b7  over the 64 MB limit - skipped": return "  \u00b7  supera el l\u00edmite de 64 MB: se salta";
            default: return en;
        }
    }

    private static String Pt(String en) {
        switch (en) {
            case "Home": return "In\u00edcio";
            case "Language": return "Idioma";
            case "\u25b6  Game": return "\u25b6  Jogo";
            case "Music": return "M\u00fasica";
            case "About": return "Sobre";
            case "Loading\u2026": return "Carregando\u2026";
            case "Storage access needed": return "\u00c9 preciso acesso ao armazenamento";
            case "The game, its mods and your music live in the THPS4 folder on the phone's storage. Android asks for all-files access once - allow it, then come back here.": return "O jogo, os mods e suas m\u00fasicas ficam na pasta THPS4 do armazenamento do telefone. O Android pede acesso a todos os arquivos uma \u00fanica vez: permita e volte aqui.";
            case "Grant storage access": return "Permitir acesso";
            case "Game folder: ": return "Pasta do jogo: ";
            case "Status: looks valid (found the game).": return "Status: v\u00e1lido (jogo encontrado).";
            case "\u2b07  Import ISO": return "\u2b07  Importar ISO";
            case "Status: game not found.": return "Status: jogo n\u00e3o encontrado.";
            case "Put the extracted game (default.xbe with its data folder) or the game ISO in the THPS4 folder above, or in its assets subfolder. An SD card or USB stick works too: a THPS4 folder at its root.": return "Coloque o jogo extra\u00eddo (default.xbe com a pasta data) ou a ISO do jogo na pasta THPS4 acima, ou na subpasta assets dela. Um cart\u00e3o SD ou pendrive tamb\u00e9m serve: uma pasta THPS4 na raiz.";
            case "The app moves the ISO into the THPS4 folder for you (the folder is created if needed).": return "O app move a ISO para a pasta THPS4 para voc\u00ea (a pasta \u00e9 criada se precisar).";
            case "The game's own texts, menus and dialogue. Applies when the game starts.": return "Os textos, menus e di\u00e1logos do pr\u00f3prio jogo. Vale quando o jogo inicia.";
            case "Mods: \u2026": return "Mods: \u2026";
            case "Tap to manage mod packs.": return "Toque para gerenciar os pacotes de mod.";
            case "Custom tracks: \u2026": return "Faixas personalizadas: \u2026";
            case "Tap to manage your soundtrack.": return "Toque para gerenciar sua trilha sonora.";
            case "Mods on: ": return "Mods ativos: ";
            case " of ": return " de ";
            case "Custom tracks: ": return "Faixas personalizadas: ";
            case " songs in ": return " faixas em ";
            case " albums (": return " \u00e1lbuns (";
            case "\u25b6  Launch Game": return "\u25b6  Iniciar jogo";
            case "Graphics, controls and the full settings live inside the game: Options \u2192 Port Settings, and the settings panel on L3+R3.": return "Os gr\u00e1ficos, os controles e todas as configura\u00e7\u00f5es ficam dentro do jogo: Op\u00e7\u00f5es \u2192 Ajustes do Port, e o painel de configura\u00e7\u00f5es no L3+R3.";
            case "About this port": return "Sobre este port";
            case "An unofficial Android port of Tony Hawk's Pro Skater 4 (Xbox), recompiled for your phone. The game is not included \u2014 this port reads your original game data or ISO file from the THPS4 folder.": return "Um port n\u00e3o oficial de Tony Hawk's Pro Skater 4 (Xbox) para Android, recompilado para o seu telefone. O jogo n\u00e3o est\u00e1 inclu\u00eddo \u2014 este port l\u00ea os dados originais do seu jogo ou o arquivo ISO da pasta THPS4.";
            case "Version ": return "Vers\u00e3o ";
            case "Support via Sociabuzz": return "Apoie via Sociabuzz";
            case "Support via Saweria": return "Apoie via Saweria";
            case "Updates": return "Atualiza\u00e7\u00f5es";
            case "New builds are published on the project's GitHub Releases. Every build is signed with the same key, so an update installs over this version.": return "As novas vers\u00f5es s\u00e3o publicadas no GitHub Releases do projeto. Todas t\u00eam a mesma assinatura, ent\u00e3o uma atualiza\u00e7\u00e3o instala por cima desta vers\u00e3o.";
            case "Checking for updates\u2026": return "Verificando atualiza\u00e7\u00f5es\u2026";
            case "Update available: THPS4 ": return "Atualiza\u00e7\u00e3o dispon\u00edvel: THPS4 ";
            case "You're on the latest version.": return "Voc\u00ea j\u00e1 est\u00e1 na vers\u00e3o mais recente.";
            case "No release found yet.": return "Nenhuma vers\u00e3o encontrada ainda.";
            case "Check failed - no connection.": return "A verifica\u00e7\u00e3o falhou: sem conex\u00e3o.";
            case "Not checked yet.": return "Ainda n\u00e3o verificado.";
            case "never": return "nunca";
            case "Last check: ": return "\u00daltima verifica\u00e7\u00e3o: ";
            case "Check for updates": return "Verificar atualiza\u00e7\u00f5es";
            case "Check automatically": return "Verificar automaticamente";
            case "When the launcher opens": return "Ao abrir o launcher";
            case "Open Releases page": return "Abrir a p\u00e1gina de Releases";
            case "Cancel": return "Cancelar";
            case "Open THPS4 ": return "Abrir THPS4 ";
            case "Download again": return "Baixar de novo";
            case "Download ": return "Baixar ";
            case "Download incomplete - try again.": return "Download incompleto: tente de novo.";
            case "Download failed - check your connection.": return "O download falhou: verifique sua conex\u00e3o.";
            case "Could not open the file.": return "N\u00e3o foi poss\u00edvel abrir o arquivo.";
            case "Loose files": return "Arquivos soltos";
            case "Order and switches": return "Ordem e ativa\u00e7\u00e3o";
            case "Switched-on mods load when the game starts. The higher a mod is in the list, the more it counts: where two mods change the same file, the upper one wins.": return "Os mods ligados carregam quando o jogo inicia. Quanto mais alto um mod estiver na lista, mais ele conta: se dois mods mexem no mesmo arquivo, o de cima vence.";
            case "A save made while playing a mod skater only loads while that mod is on. To remove a mod safely: switch to a stock skater in the game, save, then switch the mod off.": return "Um save feito jogando com um skater de mod s\u00f3 carrega enquanto o mod estiver ligado. Para remover um mod com seguran\u00e7a: troque para um skater original no jogo, salve e depois desligue o mod.";
            case "Mods folder: ": return "Pasta de mods: ";
            case "\u2b07  Import mod (.zip)": return "\u2b07  Importar mod (.zip)";
            case "No file picker available": return "Nenhum seletor de arquivos dispon\u00edvel";
            case "The game was not found yet, so its Mods folder is unknown. Put the game in place first (see Home).": return "O jogo ainda n\u00e3o foi encontrado, ent\u00e3o a pasta Mods dele n\u00e3o \u00e9 conhecida. Coloque o jogo no lugar primeiro (veja In\u00edcio).";
            case "Loading mods\u2026": return "Carregando mods\u2026";
            case "No mods yet. A mod is a folder here with a data folder (files that replace the game's), a skater.ini (a character pack), textures in a Textures folder, or a mod.ini describing it. Import a .zip to begin.": return "Ainda n\u00e3o h\u00e1 mods. Um mod \u00e9 uma pasta aqui com uma pasta data (arquivos que substituem os do jogo), um skater.ini (um pacote de personagem), texturas numa pasta Textures, ou um mod.ini que o descreve. Importe um .zip para come\u00e7ar.";
            case "\u2713  The switched-on mods work together.": return "\u2713  Os mods ligados funcionam juntos.";
            case "by ": return "por ";
            case " files": return " arquivos";
            case "on": return "ligado";
            case "off": return "desligado";
            case "Author ": return "Autor ";
            case " files\n": return " arquivos\n";
            case "Folder: ": return "Pasta: ";
            case "Close": return "Fechar";
            case "Open page": return "Abrir p\u00e1gina";
            case "Delete this mod?": return "Excluir este mod?";
            case "\n\nThe folder and everything in it is deleted from the Mods folder. This cannot be undone.": return "\n\nA pasta e tudo dentro dela \u00e9 exclu\u00eddo da pasta Mods. N\u00e3o d\u00e1 para desfazer.";
            case "Delete": return "Excluir";
            case "Looking for ISO files\u2026": return "Procurando arquivos ISO\u2026";
            case "Browse for a file\u2026": return "Procurar um arquivo\u2026";
            case "Move which ISO into the THPS4 folder?": return "Mover qual ISO para a pasta THPS4?";
            case "That is not an .iso file": return "Isso n\u00e3o \u00e9 um arquivo .iso";
            case "That ISO is already in the THPS4 folder": return "Essa ISO j\u00e1 est\u00e1 na pasta THPS4";
            case "Replace the existing ISO?": return "Substituir a ISO atual?";
            case "Replace": return "Substituir";
            case "Moving ISO\u2026": return "Movendo a ISO\u2026";
            case "Import ISO": return "Importar ISO";
            case "Moving ISO\u2026 ": return "Movendo a ISO\u2026 ";
            case "ISO moved - the game is ready": return "ISO movida: o jogo est\u00e1 pronto";
            case "ISO copied, but the original could not be deleted - you can delete it yourself": return "ISO copiada, mas o original n\u00e3o p\u00f4de ser exclu\u00eddo: voc\u00ea pode exclu\u00ed-lo.";
            case "Import failed": return "A importa\u00e7\u00e3o falhou";
            case "Game not found - no Mods folder yet": return "Jogo n\u00e3o encontrado: ainda n\u00e3o h\u00e1 pasta Mods";
            case "Importing\u2026": return "Importando\u2026";
            case "Imported ": return "Importado: ";
            case "Import failed (not a valid zip?)": return "A importa\u00e7\u00e3o falhou (n\u00e3o \u00e9 um zip v\u00e1lido?)";
            case "Custom Soundtrack": return "Trilha sonora personalizada";
            case "Music source": return "Fonte da m\u00fasica";
            case "The game's soundtrack, or your own songs played directly by the port. Applies when the game starts.": return "A trilha sonora do jogo, ou suas pr\u00f3prias m\u00fasicas tocadas diretamente pelo port. Vale quando o jogo inicia.";
            case "Game soundtrack": return "Trilha sonora do jogo";
            case "My Music": return "Minha m\u00fasica";
            case "Custom tracks": return "Faixas personalizadas";
            case "Your songs are played by the port itself, straight from the Music folder - no conversion. The game's own music steps aside automatically, and the volume follows the game's Music Volume slider. Loose songs form one album; each subfolder is an album of its own. Switched-off songs are skipped.": return "O port toca suas m\u00fasicas direto da pasta Music, sem convers\u00e3o. A m\u00fasica do jogo sai de cena automaticamente e o volume segue o controle Music Volume do jogo. M\u00fasicas soltas formam um \u00e1lbum; cada subpasta \u00e9 um \u00e1lbum pr\u00f3prio. M\u00fasicas desligadas s\u00e3o puladas.";
            case "Your songs play inside the game as a custom soundtrack: Sound Options \u2192 Soundtrack. Loose songs in the Music folder form one album; each subfolder is an album of its own, named after the folder. Switched-off songs are skipped when the game builds the soundtrack at start.": return "Suas m\u00fasicas tocam dentro do jogo como trilha personalizada: Sound Options \u2192 Soundtrack. M\u00fasicas soltas na pasta Music formam um \u00e1lbum; cada subpasta \u00e9 um \u00e1lbum pr\u00f3prio, com o nome da pasta. M\u00fasicas desligadas s\u00e3o puladas quando o jogo monta a trilha ao iniciar.";
            case "Music folder: ": return "Pasta de m\u00fasicas: ";
            case "Limits": return "Limites";
            case "\u2022  Formats: MP3, M4A/AAC, OGG, Opus, FLAC, WAV - whatever the phone itself can play (WMA stays with the game soundtrack source)": return "\u2022  Formatos: MP3, M4A/AAC, OGG, Opus, FLAC, WAV: o que o pr\u00f3prio telefone consegue tocar (WMA fica com a trilha do jogo)";
            case "\u2022  No per-song size or duration limit": return "\u2022  Sem limite de tamanho nem dura\u00e7\u00e3o por faixa";
            case "\u2022  Format: WMA only - the game's own decoder plays them (44.1 kHz stereo is what the original Xbox dashboard used)": return "\u2022  Formato: s\u00f3 WMA: quem toca \u00e9 o decodificador do pr\u00f3prio jogo (44,1 kHz est\u00e9reo era o que o dashboard original do Xbox usava)";
            case "\u2022  Max size: 64 MB per song": return "\u2022  Tamanho m\u00e1ximo: 64 MB por faixa";
            case "\u2022  Max amount: 500 songs per album, 100 albums": return "\u2022  Quantidade m\u00e1xima: 500 faixas por \u00e1lbum, 100 \u00e1lbuns";
            case "\u2022  Duration: read from each song file - there is no per-song duration limit": return "\u2022  Dura\u00e7\u00e3o: lida de cada arquivo: n\u00e3o h\u00e1 limite de dura\u00e7\u00e3o por faixa";
            case "Loading songs\u2026": return "Carregando faixas\u2026";
            case "No songs yet. Copy MP3 (or M4A, OGG, FLAC, WAV) files into the Music folder above and they will show up here.": return "Ainda n\u00e3o h\u00e1 faixas. Copie arquivos MP3 (ou M4A, OGG, FLAC, WAV) para a pasta Music acima e eles aparecem aqui.";
            case "No songs yet. Copy .wma files into the Music folder above and they will show up here.": return "Ainda n\u00e3o h\u00e1 faixas. Copie arquivos .wma para a pasta Music acima e eles aparecem aqui.";
            case " songs \u00b7 ": return " faixas \u00b7 ";
            case "  \u00b7  over the 64 MB limit - skipped": return "  \u00b7  acima do limite de 64 MB: pulada";
            default: return en;
        }
    }

    private File gameDir;    // the folder holding the game (xbe + data, or the ISO)
    private File modsDir;    // gameDir/Mods once known
    private boolean scanned;

    // ------------------------------------------------------------------ setup

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsFile = new File(getExternalFilesDir(null), "settings.toml");
        buildShell();
        showTab(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        askStorageAccess();
        io.execute(() -> {
            loadSettings();
            resolveGameDir();
            scanned = true;
            maybeAutoUpdateCheck();
            ui.post(() -> showTab(tab));
        });
    }

    private void askStorageAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                }
            }
        } else if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(18), dp(20), dp(10));
        TextView kicker = text("TONY HAWK'S PRO SKATER 4", 12, MUTED);
        kicker.setLetterSpacing(0.12f);
        header.addView(kicker);
        header.addView(text("THPS4", 34, TEXT));
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(4), dp(14), dp(18));
        scroll.addView(content);
        root.addView(scroll);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(0xFF0A1017);
        nav.setPadding(dp(6), dp(6), dp(6), dp(8));
        String[] labels = {"Home", "Mods", "Music", "About"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            Button b = new Button(this);
            b.setText(labels[i]);
            b.setTextSize(14);
            b.setAllCaps(false);
            b.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1f));
            b.setOnClickListener(v -> showTab(idx));
            navButtons[i] = b;
            nav.addView(b);
        }
        root.addView(nav);
        setContentView(root);
    }

    private void showTab(int idx) {
        tab = idx;
        String[] navLabels = {L("Home", "Beranda"), L("Mods", "Mod"), L("Music", "Musik"), L("About", "Tentang")};
        for (int i = 0; i < navButtons.length; i++)
            if (navButtons[i] != null) navButtons[i].setText(navLabels[i]);
        for (int i = 0; i < navButtons.length; i++) {
            navButtons[i].setTextColor(i == idx ? ACCENT : MUTED);
            navButtons[i].setBackground(i == idx ? rounded(CARD2, 12) : null);
        }
        content.removeAllViews();
        if (!scanned) {
            content.addView(text(L("Loading…", "Memuat…"), 15, MUTED));
            return;
        }
        switch (idx) {
            case 1: showMods(); break;
            case 2: showMusic(); break;
            case 3: showAbout(); break;
            default: showHome(); break;
        }
    }

    // -------------------------------------------------------------- settings

    private void loadSettings() {
        settings.clear();
        if (settingsFile == null || !settingsFile.isFile()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(settingsFile))) {
            String line;
            while ((line = r.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2)
                    val = val.substring(1, val.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
                settings.put(key, val);
            }
        } catch (Exception ignored) {
        }
        try {
            uiLang = Integer.parseInt(settings.getOrDefault("language", "0"));
        } catch (Exception ignored) {
            uiLang = 0;
        }
    }

    private void saveSetting(String key, String value, boolean quoted) {
        settings.put(key, value);
        io.execute(() -> {
            try {
                List<String> lines = new ArrayList<>();
                boolean done = false;
                if (settingsFile.isFile()) {
                    try (BufferedReader r = new BufferedReader(new FileReader(settingsFile))) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            int eq = line.indexOf('=');
                            if (!done && eq > 0 && line.substring(0, eq).trim().equals(key)) {
                                lines.add(key + " = " + (quoted ? quote(value) : value));
                                done = true;
                            } else {
                                lines.add(line);
                            }
                        }
                    }
                }
                if (!done) lines.add(key + " = " + (quoted ? quote(value) : value));
                settingsFile.getParentFile().mkdirs();
                try (FileWriter w = new FileWriter(settingsFile)) {
                    for (String l : lines) w.write(l + "\n");
                }
            } catch (Exception ignored) {
            }
        });
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private List<String> listSetting(String key) {
        List<String> out = new ArrayList<>();
        String v = settings.get(key);
        if (v != null)
            for (String part : v.split("\\|"))
                if (!part.isEmpty()) out.add(part);
        return out;
    }

    private void saveListSetting(String key, List<String> items) {
        saveSetting(key, String.join("|", items), true);
    }

    // ------------------------------------------------------------- game dirs

    private static File childIgnoreCase(File dir, String name) {
        if (dir == null) return null;
        File[] kids = dir.listFiles();
        if (kids == null) return null;
        for (File k : kids)
            if (k.getName().equalsIgnoreCase(name)) return k;
        return null;
    }

    private static boolean hasGameData(File dir) {
        File data = childIgnoreCase(dir, "data");
        return data != null && data.isDirectory();
    }

    // mirrors the native search (native/game_files.cpp Search): the extracted
    // game (default.xbe + data) or any .iso, in the THPS4 folder, its assets
    // subfolder, the same on removable volumes, and the app's own assets
    private void resolveGameDir() {
        List<File> candidates = new ArrayList<>();
        candidates.add(new File(SHARED));
        candidates.add(new File(SHARED, "assets"));
        File storage = new File("/storage");
        File[] vols = storage.listFiles();
        if (vols != null)
            for (File v : vols) {
                if (v.getName().equals("emulated") || v.getName().equals("self")) continue;
                candidates.add(new File(v, "THPS4"));
                candidates.add(new File(v, "THPS4/assets"));
            }
        File ext = getExternalFilesDir(null);
        if (ext != null) candidates.add(new File(ext, "assets"));
        gameDir = null;
        for (File c : candidates) {
            if (!c.isDirectory()) continue;
            File xbe = childIgnoreCase(c, "default.xbe");
            if (xbe != null && xbe.isFile() && hasGameData(c)) { gameDir = c; break; }
            File[] files = c.listFiles();
            if (files != null)
                for (File f : files)
                    if (f.isFile() && f.getName().toLowerCase(Locale.US).endsWith(".iso")) {
                        gameDir = c;
                        break;
                    }
            if (gameDir != null) break;
        }
        modsDir = null;
        if (gameDir != null) {
            File m = childIgnoreCase(gameDir, "mods");
            modsDir = m != null ? m : new File(gameDir, "Mods");
        }
    }

    // ------------------------------------------------------------ ui helpers

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(rounded(CARD, 16));
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        c.setLayoutParams(lp);
        return c;
    }

    private Button accentButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(0xFF1A1400);
        b.setBackground(rounded(ACCENT, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        lp.setMargins(0, dp(6), 0, dp(6));
        b.setLayoutParams(lp);
        return b;
    }

    private Button plainButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTextColor(TEXT);
        b.setBackground(rounded(CARD2, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        lp.setMargins(0, dp(4), 0, dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    private static String fmtSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.0f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024)
            return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024));
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String fmtDuration(long ms) {
        if (ms <= 0) return "--:--";
        long s = ms / 1000;
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    private static long dirSize(File f) {
        if (f == null || !f.exists()) return 0;
        if (f.isFile()) return f.length();
        long total = 0;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) total += dirSize(k);
        return total;
    }

    // ------------------------------------------------------------------ home

    private void showHome() {
        boolean storageOk = Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager();
        if (!storageOk) {
            LinearLayout c = card();
            c.addView(text(L("Storage access needed", "Perlu akses penyimpanan"), 17, TEXT));
            c.addView(text(L("The game, its mods and your music live in the THPS4 folder on the "
                    + "phone's storage. Android asks for all-files access once - allow it, "
                    + "then come back here.", "Game, mod, dan musik kamu tersimpan di folder "
                    + "THPS4 di penyimpanan HP. Android meminta izin akses semua file satu "
                    + "kali - izinkan, lalu kembali ke sini."), 14, MUTED));
            Button grant = accentButton(L("Grant storage access", "Beri akses penyimpanan"));
            grant.setOnClickListener(v -> askStorageAccess());
            c.addView(grant);
            content.addView(c);
        }

        LinearLayout game = card();
        game.addView(text(L("▶  Game", "▶  Game"), 18, TEXT));
        if (gameDir != null) {
            game.addView(text(L("Game folder: ", "Folder game: ") + gameDir.getAbsolutePath(), 14, MUTED));
            TextView st = text(L("Status: looks valid (found the game).", "Status: valid (game ditemukan)."), 14, GOOD);
            game.addView(st);
            if (storageOk) {
                Button imp = plainButton(L("⬇  Import ISO", "⬇  Impor ISO"));
                imp.setOnClickListener(v -> startIsoImport());
                game.addView(imp);
            }
        } else {
            game.addView(text(L("Game folder: ", "Folder game: ") + SHARED, 14, MUTED));
            TextView st = text(L("Status: game not found.", "Status: game tidak ditemukan."), 14, BAD);
            game.addView(st);
            game.addView(text(L("Put the extracted game (default.xbe with its data folder) or "
                    + "the game ISO in the THPS4 folder above, or in its assets subfolder. "
                    + "An SD card or USB stick works too: a THPS4 folder at its root.",
                    "Taruh game yang sudah diekstrak (default.xbe beserta folder data-nya) "
                    + "atau ISO game di folder THPS4 di atas, atau di subfolder assets-nya. "
                    + "Kartu SD atau USB juga bisa: folder THPS4 di akar medianya."), 14, MUTED));
            if (storageOk) {
                Button imp = accentButton(L("⬇  Import ISO", "⬇  Impor ISO"));
                imp.setOnClickListener(v -> startIsoImport());
                game.addView(imp);
                game.addView(text(L("The app moves the ISO into the THPS4 folder for you "
                        + "(the folder is created if needed).", "Aplikasi memindahkan ISO "
                        + "ke folder THPS4 untukmu (folder dibuat kalau belum ada)."), 13, MUTED));
            }
        }
        content.addView(game);

        LinearLayout lang = card();
        lang.addView(text(L("Language", "Bahasa"), 18, TEXT));
        lang.addView(text(L("The game's own texts, menus and dialogue. Applies when the "
                + "game starts.", "Teks, menu, dan dialog di dalam game itu sendiri. "
                + "Berlaku saat game dimulai."), 14, MUTED));
        int cur = 0;
        try {
            cur = Integer.parseInt(settings.getOrDefault("language", "0"));
        } catch (Exception ignored) {
        }
        // four game languages; the launcher's own UI follows the pick
        String[] names = {"English", "Bahasa Indonesia", "Español", "Português"};
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 2; c++) {
                final int idx = r * 2 + c;
                Button b = new Button(this);
                b.setText(names[idx]);
                b.setAllCaps(false);
                b.setTextSize(15);
                b.setTextColor(idx == cur ? 0xFF1A1400 : TEXT);
                b.setBackground(rounded(idx == cur ? ACCENT : CARD2, 12));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
                lp.setMargins(dp(3), dp(8), dp(3), 0);
                b.setLayoutParams(lp);
                b.setOnClickListener(v -> {
                    saveSetting("language", String.valueOf(idx), false);
                    uiLang = idx;
                    showTab(0);
                });
                row.addView(b);
            }
            lang.addView(row);
        }
        content.addView(lang);

        // summaries, filled in by a background pass (sizes and durations cost io)
        TextView modsLine = text(L("Mods: …", "Mod: …"), 15, TEXT);
        modsLine.setOnClickListener(v -> showTab(1));
        LinearLayout modsCard = card();
        modsCard.addView(modsLine);
        modsCard.addView(text(L("Tap to manage mod packs.", "Ketuk untuk mengatur paket mod."), 13, MUTED));
        content.addView(modsCard);
        TextView musicLine = text(L("Custom tracks: …", "Lagu kustom: …"), 15, TEXT);
        musicLine.setOnClickListener(v -> showTab(2));
        LinearLayout musicCard = card();
        musicCard.addView(musicLine);
        musicCard.addView(text(L("Tap to manage your soundtrack.", "Ketuk untuk mengelola soundtrack kamu."), 13, MUTED));
        content.addView(musicCard);
        io.execute(() -> {
            List<Pack> packs = scanPacks();
            Set<String> off = new HashSet<>(listSetting("mods_disabled"));
            int on = 0;
            for (Pack p : packs) if (!off.contains(p.key)) on++;
            final int onCount = on;
            MusicLib lib = scanMusic();
            ui.post(() -> {
                if (tab != 0) return;
                modsLine.setText(L("Mods on: ", "Mod aktif: ") + onCount
                        + L(" of ", " dari ") + packs.size());
                musicLine.setText(L("Custom tracks: ", "Lagu kustom: ") + lib.tracks
                        + L(" songs in ", " lagu dalam ") + lib.albums.size()
                        + L(" albums (", " album (") + fmtSize(lib.bytes) + ")");
            });
        });

        Button launch = accentButton(L("▶  Launch Game", "▶  Mulai Game"));
        launch.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
        content.addView(launch);
        content.addView(text(L("Graphics, controls and the full settings live inside the game: "
                + "Options → Port Settings, and the settings panel on L3+R3.",
                "Grafis, kontrol, dan semua pengaturan ada di dalam game: "
                + "Opsi → Pengaturan Port, dan panel pengaturan pada L3+R3."), 13, MUTED));
    }

    // ----------------------------------------------------------------- about

    private void showAbout() {
        LinearLayout c = card();
        c.addView(text(L("About this port", "Tentang port ini"), 18, TEXT));
        c.addView(text(L("An unofficial Android port of Tony Hawk's Pro Skater 4 (Xbox), "
                + "recompiled for your phone. The game is not included \u2014 this port reads "
                + "your original game data or ISO file from the THPS4 folder.",
                "Port Android tidak resmi dari "
                + "Tony Hawk's Pro Skater 4 (Xbox), dikompilasi ulang untuk HP kamu. "
                + "Game tidak disertakan \u2014 port ini membaca data game asli atau file ISO "
                + "milikmu dari folder THPS4."), 14, MUTED));
        String version = "?";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
        }
        c.addView(text(L("Version ", "Versi ") + version, 14, MUTED));
        content.addView(c);
        content.addView(updatesCard(version));

        LinearLayout credit = card();
        credit.addView(text("Port by UNKNUW", 18, ACCENT));
        Button gh = plainButton("GitHub — UnknuwProjects");
        gh.setOnClickListener(v -> openUrl("https://github.com/UnknuwProjects"));
        credit.addView(gh);
        Button sb = plainButton(L("Support via Sociabuzz", "Dukung via Sociabuzz"));
        sb.setOnClickListener(v -> openUrl("https://sociabuzz.com/unknuw/tribe"));
        credit.addView(sb);
        Button sw = plainButton(L("Support via Saweria", "Dukung via Saweria"));
        sw.setOnClickListener(v -> openUrl("https://saweria.co/Unknuw"));
        credit.addView(sw);
        content.addView(credit);
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }

    // --------------------------------------------------------------- updates

    // The update check lives here, on the About tab: it asks the project's
    // public release repository for the newest release and compares its
    // tag with this build's version. Development stays in a private
    // repository; the public one (THPS4-Android-Releases) carries only
    // the README and the stable releases, so the check and the download
    // work without any sign-in.
    private static final String kReleasesApi =
            "https://api.github.com/repos/UnknuwProjects/THPS4-Android-Releases/releases/latest";
    private static final String kReleasesPage =
            "https://github.com/UnknuwProjects/THPS4-Android-Releases/releases";
    private static final int UPD_NONE = 0, UPD_LATEST = 1, UPD_AVAILABLE = 2,
            UPD_NO_RELEASE = 3, UPD_NO_CONNECTION = 4;

    private volatile boolean updateChecking;

    private SharedPreferences updatePrefs() {
        return getSharedPreferences("launcher_updates", MODE_PRIVATE);
    }

    private LinearLayout updatesCard(String version) {
        SharedPreferences prefs = updatePrefs();
        int state = prefs.getInt("state", UPD_NONE);
        String tag = prefs.getString("tag", "");
        LinearLayout c = card();
        c.addView(text(L("Updates", "Pembaruan"), 18, TEXT));
        c.addView(text(L("New builds are published on the project's GitHub Releases. "
                + "Every build is signed with the same key, so an update installs "
                + "over this version.", "Build baru diterbitkan di GitHub Releases "
                + "proyek ini. Semua build ditandatangani dengan kunci yang sama, "
                + "jadi pembaruan terpasang di atas versi ini."), 14, MUTED));
        if (updateChecking) {
            c.addView(text(L("Checking for updates\u2026", "Memeriksa pembaruan\u2026"), 14, MUTED));
        } else if (state == UPD_AVAILABLE) {
            c.addView(text(L("Update available: THPS4 ", "Pembaruan tersedia: THPS4 ") + tag, 15, GOOD));
            String notes = prefs.getString("notes", "");
            if (!notes.isEmpty()) {
                TextView n = text(notes, 13, MUTED);
                n.setMaxLines(14);
                c.addView(n);
            }
            addUpdateDownloadViews(c, tag, prefs);
        } else if (state == UPD_LATEST) {
            c.addView(text(L("You're on the latest version.", "Kamu sudah di versi terbaru."), 14, MUTED));
        } else if (state == UPD_NO_RELEASE) {
            c.addView(text(L("No release found yet.", "Belum ada rilis ditemukan."), 14, MUTED));
        } else if (state == UPD_NO_CONNECTION) {
            c.addView(text(L("Check failed - no connection.", "Pemeriksaan gagal - tidak ada koneksi."), 14, MUTED));
        } else {
            c.addView(text(L("Not checked yet.", "Belum diperiksa."), 14, MUTED));
        }
        long last = prefs.getLong("last_check", 0);
        String when = last > 0
                ? new SimpleDateFormat("d MMM, HH:mm", Locale.US).format(new Date(last))
                : L("never", "belum pernah");
        c.addView(text(L("Version ", "Versi ") + version + "  \u00b7  "
                + L("Last check: ", "Terakhir diperiksa: ") + when, 13, MUTED));
        Button check = plainButton(L("Check for updates", "Periksa pembaruan"));
        check.setEnabled(!updateChecking);
        check.setOnClickListener(v -> checkUpdates());
        c.addView(check);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        titles.addView(text(L("Check automatically", "Periksa otomatis"), 15, TEXT));
        titles.addView(text(L("When the launcher opens", "Saat launcher dibuka"), 13, MUTED));
        row.addView(titles);
        Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean("auto", true));
        sw.setOnCheckedChangeListener((v, checked) ->
                updatePrefs().edit().putBoolean("auto", checked).apply());
        row.addView(sw);
        c.addView(row);
        Button page = plainButton(L("Open Releases page", "Buka halaman Releases"));
        page.setOnClickListener(v -> openUrl(kReleasesPage));
        c.addView(page);
        return c;
    }

    private void maybeAutoUpdateCheck() {
        SharedPreferences prefs = updatePrefs();
        if (!prefs.getBoolean("auto", true)) return;
        long last = prefs.getLong("last_check", 0);
        if (System.currentTimeMillis() - last < 6 * 60 * 60 * 1000L) return;
        checkUpdates();
    }

    private void checkUpdates() {
        if (updateChecking) return;
        updateChecking = true;
        if (tab == 3) ui.post(() -> showTab(3));
        io.execute(() -> {
            int state = UPD_NO_CONNECTION;
            String tag = "", url = "", notes = "";
            long assetSize = 0;
            try {
                String current = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                HttpURLConnection conn = (HttpURLConnection) new URL(kReleasesApi).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "THPS4-Android");
                int code = conn.getResponseCode();
                if (code == 404) {
                    state = UPD_NO_RELEASE;
                } else if (code == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = in.readLine()) != null) sb.append(line);
                    }
                    JSONObject release = new JSONObject(sb.toString());
                    tag = release.optString("tag_name");
                    if (tag.isEmpty()) {
                        state = UPD_NO_RELEASE;
                    } else if (!isNewer(tag, current)) {
                        state = UPD_LATEST;
                    } else {
                        state = UPD_AVAILABLE;
                        notes = cleanNotes(release.optString("body"));
                        JSONArray assets = release.optJSONArray("assets");
                        if (assets != null) {
                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject a = assets.optJSONObject(i);
                                if (a != null && a.optString("name").toLowerCase(Locale.US).endsWith(".apk")) {
                                    url = a.optString("browser_download_url");
                                    assetSize = a.optLong("size");
                                    break;
                                }
                            }
                        }
                        if (url.isEmpty()) url = release.optString("html_url");
                    }
                }
            } catch (Exception ignored) {
                state = UPD_NO_CONNECTION;
            }
            updatePrefs().edit()
                    .putInt("state", state)
                    .putString("tag", tag)
                    .putString("url", url)
                    .putString("notes", notes)
                    .putLong("size", assetSize)
                    .putLong("last_check", System.currentTimeMillis())
                    .apply();
            updateChecking = false;
            if (tab == 3) ui.post(() -> showTab(3));
        });
    }

    // ------------------------------------------------------- update download

    // The update APK is downloaded inside the launcher, with progress and
    // speed, into the app's own files. When it lands, the launcher only OPENS
    // the file: the system installer (or whatever handler the player picks)
    // takes it from there. The launcher never installs anything itself and
    // holds no install permission.
    private volatile boolean dlActive, dlCancel;
    private volatile long dlDone, dlTotal, dlSpeedBps;
    private volatile String dlError;
    private ProgressBar dlBar;
    private TextView dlStatus;
    private final Runnable dlTicker = new Runnable() {
        @Override public void run() {
            if (!dlActive) return;
            updateDlViews();
            ui.postDelayed(this, 250);
        }
    };

    private File updatesDir() {
        File base = getExternalFilesDir(null);
        File dir = new File(base != null ? base : getFilesDir(), "updates");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private File downloadedApk(String tag) {
        return new File(updatesDir(), "THPS4-" + tag.replaceAll("[^A-Za-z0-9._-]", "_") + ".apk");
    }

    private void addUpdateDownloadViews(LinearLayout c, String tag, SharedPreferences prefs) {
        if (dlActive) {
            dlBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            dlBar.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            c.addView(dlBar);
            dlStatus = text("", 13, MUTED);
            c.addView(dlStatus);
            Button cancel = plainButton(L("Cancel", "Batal"));
            cancel.setOnClickListener(v -> dlCancel = true);
            c.addView(cancel);
            updateDlViews();
            ui.removeCallbacks(dlTicker);
            ui.post(dlTicker);
        } else if (downloadedApk(tag).isFile() && downloadedApk(tag).length() > 0) {
            Button open = accentButton(L("Open THPS4 ", "Buka THPS4 ") + tag);
            open.setOnClickListener(v -> openDownloadedApk(downloadedApk(tag)));
            c.addView(open);
            Button again = plainButton(L("Download again", "Unduh lagi"));
            again.setOnClickListener(v -> startUpdateDownload(tag, prefs));
            c.addView(again);
        } else {
            long size = prefs.getLong("size", 0);
            Button dl = accentButton(L("Download ", "Unduh ") + tag
                    + (size > 0 ? " (" + fmtSize(size) + ")" : ""));
            dl.setOnClickListener(v -> startUpdateDownload(tag, prefs));
            c.addView(dl);
            if (dlError != null) c.addView(text(dlError, 13, MUTED));
        }
    }

    private void updateDlViews() {
        if (dlBar != null) {
            if (dlTotal > 0) {
                dlBar.setIndeterminate(false);
                dlBar.setMax(100);
                dlBar.setProgress((int) (dlDone * 100 / dlTotal));
            } else {
                dlBar.setIndeterminate(true);
            }
        }
        if (dlStatus != null) {
            dlStatus.setText(fmtSize(dlDone)
                    + (dlTotal > 0 ? " / " + fmtSize(dlTotal) : "")
                    + (dlTotal > 0 ? "  \u00b7  " + (dlDone * 100 / dlTotal) + "%" : "")
                    + (dlSpeedBps > 0 ? "  \u00b7  " + fmtSize(dlSpeedBps) + "/s" : ""));
        }
    }

    private void startUpdateDownload(String tag, SharedPreferences prefs) {
        final String url = prefs.getString("url", "");
        if (url.isEmpty() || dlActive) return;
        final File target = downloadedApk(tag);
        dlActive = true;
        dlCancel = false;
        dlError = null;
        dlDone = 0;
        dlTotal = prefs.getLong("size", 0);
        dlSpeedBps = 0;
        if (tab == 3) showTab(3);
        io.execute(() -> {
            File tmp = new File(target.getParentFile(), target.getName() + ".part");
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", "THPS4-Android");
                conn.setRequestProperty("Accept", "application/octet-stream");
                if (conn.getResponseCode() != 200) throw new Exception("HTTP " + conn.getResponseCode());
                long total = conn.getContentLengthLong();
                if (total > 0) dlTotal = total;
                long done = 0, sampleBytes = 0;
                long sampleTime = System.currentTimeMillis();
                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buf)) >= 0) {
                        if (dlCancel) break;
                        out.write(buf, 0, n);
                        done += n;
                        sampleBytes += n;
                        dlDone = done;
                        long now = System.currentTimeMillis();
                        if (now - sampleTime >= 500) {
                            dlSpeedBps = sampleBytes * 1000 / (now - sampleTime);
                            sampleBytes = 0;
                            sampleTime = now;
                        }
                    }
                }
                if (dlCancel) {
                    tmp.delete();
                } else if (dlTotal > 0 && done != dlTotal) {
                    tmp.delete();
                    dlError = L("Download incomplete - try again.", "Unduhan tidak lengkap - coba lagi.");
                } else {
                    if (target.exists()) target.delete();
                    if (!tmp.renameTo(target)) throw new Exception("rename failed");
                }
            } catch (Exception e) {
                tmp.delete();
                if (!dlCancel) dlError = L("Download failed - check your connection.",
                        "Unduhan gagal - periksa koneksimu.");
            }
            dlActive = false;
            ui.post(() -> { if (tab == 3) showTab(3); });
        });
    }

    private void openDownloadedApk(File apk) {
        try {
            Uri uri = new Uri.Builder()
                    .scheme("content")
                    .authority("com.unknuw.thps4.unofficial.updates")
                    .appendPath(apk.getName())
                    .build();
            Intent it = new Intent(Intent.ACTION_VIEW);
            it.setDataAndType(uri, "application/vnd.android.package-archive");
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(it);
        } catch (Exception e) {
            Toast.makeText(this, L("Could not open the file.", "Tidak bisa membuka file."),
                    Toast.LENGTH_LONG).show();
        }
    }

    // Release notes are written as markdown for the GitHub page; on the card
    // the raw markers only get in the way, so they are flattened to plain text.
    private static String cleanNotes(String body) {
        StringBuilder out = new StringBuilder();
        for (String line : body.split("\n")) {
            String s = line.trim();
            s = s.replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1");
            s = s.replace("**", "").replace("__", "").replace("`", "");
            if (s.startsWith("#")) s = s.replaceFirst("^#+\\s*", "");
            if (s.startsWith("- ") || s.startsWith("* ")) s = "\u2022 " + s.substring(2);
            out.append(s).append("\n");
        }
        String r = out.toString().replaceAll("\n{3,}", "\n\n").trim();
        if (r.length() > 1500) r = r.substring(0, 1500) + "...";
        return r;
    }

    // dotted numeric compare, a leading v is decoration: v1.10 is newer than 1.9
    private static boolean isNewer(String tag, String current) {
        int[] a = versionParts(tag), b = versionParts(current == null ? "" : current);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0, y = i < b.length ? b[i] : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int[] versionParts(String version) {
        String v = version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
        // only the number runs count: "beta-1.12" is [1, 12], not [0, 1, 12] -
        // the empty run before the first number used to shift everything and
        // made a beta look older than the public releases it already contains
        java.util.ArrayList<Integer> parts = new java.util.ArrayList<>();
        for (String s : v.split("[^0-9]+")) {
            if (s.isEmpty()) continue;
            try {
                parts.add(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
            }
        }
        int[] out = new int[parts.size()];
        for (int i = 0; i < out.length; i++) out[i] = parts.get(i);
        return out;
    }

    // ------------------------------------------------------------------ mods

    private static class Pack {
        String key;          // folder name, or <loose> for the Mods folder itself
        File dir;
        String name, version, author, description, url;
        Bitmap cover;
        long bytes;
        int files;
    }

    private static Map<String, String> readIni(File f) {
        Map<String, String> out = new HashMap<>();
        if (f == null || !f.isFile()) return out;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith(";") || t.startsWith("#") || t.startsWith("[")) continue;
                int eq = t.indexOf('=');
                if (eq <= 0) continue;
                out.put(t.substring(0, eq).trim().toLowerCase(Locale.US),
                        t.substring(eq + 1).trim());
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static int countFiles(File f) {
        if (f == null || !f.exists()) return 0;
        if (f.isFile()) return 1;
        int n = 0;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) n += countFiles(k);
        return n;
    }

    private Bitmap loadCover(File packDir, Map<String, String> ini) {
        List<File> tries = new ArrayList<>();
        String image = ini.get("image");
        if (image != null) tries.add(new File(packDir, image));
        for (String base : new String[]{"cover", "preview", "logo", "icon"})
            for (String ext : new String[]{".png", ".jpg", ".jpeg", ".webp"})
                tries.add(new File(packDir, base + ext));
        for (File t : tries) {
            if (!t.isFile()) continue;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(t.getAbsolutePath(), o);
            if (o.outWidth <= 0) continue;
            o.inSampleSize = 1;
            while (o.outWidth / o.inSampleSize > 1024) o.inSampleSize *= 2;
            o.inJustDecodeBounds = false;
            Bitmap b = BitmapFactory.decodeFile(t.getAbsolutePath(), o);
            if (b != null) return b;
        }
        return null;
    }

    // packs in the player's priority order: mods_order first (in its order),
    // then the packs it has never seen, by name; the loose root joins in
    private List<Pack> scanPacks() {
        List<Pack> found = new ArrayList<>();
        if (modsDir == null || !modsDir.isDirectory()) return found;
        List<File> dirs = new ArrayList<>();
        File[] kids = modsDir.listFiles();
        if (kids != null)
            for (File k : kids) {
                if (!k.isDirectory()) continue;
                if (childIgnoreCase(k, "data") != null || childIgnoreCase(k, "skater.ini") != null
                        || childIgnoreCase(k, "mod.ini") != null
                        || childIgnoreCase(k, "textures") != null)
                    dirs.add(k);
            }
        Collections.sort(dirs, Comparator.comparing(f -> f.getName().toLowerCase(Locale.US)));
        // the loose root counts as a pack only when it holds mod content itself
        boolean looseHas = childIgnoreCase(modsDir, "data") != null
                || childIgnoreCase(modsDir, "skater.ini") != null
                || childIgnoreCase(modsDir, "mod.ini") != null;
        List<String> order = listSetting("mods_order");
        Map<String, Pack> byKey = new HashMap<>();
        if (looseHas) byKey.put(LOOSE, makePack(LOOSE, modsDir));
        for (File d : dirs) byKey.put(d.getName(), makePack(d.getName(), d));
        Set<String> seen = new HashSet<>();
        for (String key : order)
            if (byKey.containsKey(key) && seen.add(key)) found.add(byKey.get(key));
        List<Pack> rest = new ArrayList<>();
        for (Map.Entry<String, Pack> e : byKey.entrySet())
            if (!seen.contains(e.getKey())) rest.add(e.getValue());
        Collections.sort(rest, Comparator.comparing(p -> p.key.toLowerCase(Locale.US)));
        found.addAll(rest);
        return found;
    }

    private Pack makePack(String key, File dir) {
        Pack p = new Pack();
        p.key = key;
        p.dir = dir;
        Map<String, String> ini = readIni(childIgnoreCase(dir, "mod.ini"));
        p.name = ini.getOrDefault("name", LOOSE.equals(key) ? L("Loose files", "File lepas") : key);
        p.version = ini.get("version");
        p.author = ini.get("author");
        p.description = ini.get("description");
        p.url = ini.get("url");
        p.cover = loadCover(dir, ini);
        p.bytes = dirSize(dir);
        p.files = countFiles(dir);
        return p;
    }

    private void showMods() {
        LinearLayout info = card();
        info.addView(text(L("Order and switches", "Urutan dan sakelar"), 18, TEXT));
        info.addView(text(L("Switched-on mods load when the game starts. The higher a mod is "
                + "in the list, the more it counts: where two mods change the same file, "
                + "the upper one wins.", "Mod yang aktif dimuat saat game dimulai. Semakin "
                + "tinggi posisi mod di daftar, semakin diutamakan: jika dua mod mengubah "
                + "file yang sama, yang di atas yang dipakai."), 14, MUTED));
        info.addView(text(L("A save made while playing a mod skater only loads while that "
                + "mod is on. To remove a mod safely: switch to a stock skater in the game, "
                + "save, then switch the mod off.", "Save yang dibuat saat memakai skater "
                + "mod hanya bisa dimuat selama mod itu aktif. Mau melepas mod dengan aman: "
                + "ganti ke skater bawaan di dalam game, simpan, baru matikan mod-nya."), 14, MUTED));
        if (modsDir != null)
            info.addView(text(L("Mods folder: ", "Folder mod: ") + modsDir.getAbsolutePath(), 13, MUTED));
        content.addView(info);

        Button importBtn = plainButton(L("⬇  Import mod (.zip)", "⬇  Impor mod (.zip)"));
        importBtn.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/zip");
            try {
                startActivityForResult(i, REQ_ZIP);
            } catch (Exception e) {
                Toast.makeText(this, L("No file picker available", "Pemilih file tidak tersedia"), Toast.LENGTH_SHORT).show();
            }
        });
        content.addView(importBtn);
        if (gameDir == null) {
            content.addView(text(L("The game was not found yet, so its Mods folder is unknown. "
                    + "Put the game in place first (see Home).", "Game belum ditemukan, "
                    + "jadi folder Mods-nya belum diketahui. Taruh game-nya dulu "
                    + "(lihat Beranda)."), 14, MUTED));
            return;
        }

        TextView loading = text(L("Loading mods…", "Memuat mod…"), 15, MUTED);
        content.addView(loading);
        io.execute(() -> {
            List<Pack> packs = scanPacks();
            Set<String> off = new HashSet<>(listSetting("mods_disabled"));
            ui.post(() -> {
                if (tab != 1) return;
                content.removeView(loading);
                if (packs.isEmpty()) {
                    content.addView(text(L("No mods yet. A mod is a folder here with a data "
                            + "folder (files that replace the game's), a skater.ini "
                            + "(a character pack), textures in a Textures folder, or a "
                            + "mod.ini describing it. Import a .zip to begin.",
                            "Belum ada mod. Mod adalah folder di sini yang berisi folder "
                            + "data (file pengganti file game), skater.ini (paket "
                            + "karakter), tekstur di folder Textures, atau mod.ini yang "
                            + "menjelaskannya. Impor .zip untuk memulai."), 14, MUTED));
                    return;
                }
                for (int i = 0; i < packs.size(); i++)
                    content.addView(packCard(packs, i, off));
                content.addView(text(L("✓  The switched-on mods work together.", "✓  Mod yang aktif bekerja bersama."), 14, GOOD));
            });
        });
    }

    private LinearLayout packCard(List<Pack> packs, int idx, Set<String> off) {
        Pack p = packs.get(idx);
        boolean on = !off.contains(p.key);
        LinearLayout c = card();
        if (p.cover != null) {
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(p.cover);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(110)));
            c.addView(iv);
        }
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        titles.addView(text(p.name, 17, TEXT));
        List<String> meta = new ArrayList<>();
        if (p.version != null) meta.add("v" + p.version);
        if (p.author != null) meta.add(L("by ", "oleh ") + p.author);
        meta.add(fmtSize(p.bytes));
        meta.add(p.files + L(" files", " file"));
        meta.add(on ? L("on", "aktif") : L("off", "nonaktif"));
        titles.addView(text(String.join("  ·  ", meta), 13, MUTED));
        head.addView(titles);
        Switch sw = new Switch(this);
        sw.setChecked(on);
        sw.setOnCheckedChangeListener((v, checked) -> {
            List<String> disabled = listSetting("mods_disabled");
            if (checked) disabled.remove(p.key);
            else if (!disabled.contains(p.key)) disabled.add(p.key);
            saveListSetting("mods_disabled", disabled);
            ui.postDelayed(() -> showTab(1), 250);
        });
        head.addView(sw);
        c.addView(head);
        if (p.description != null && !p.description.isEmpty()) {
            TextView d = text(p.description, 14, MUTED);
            d.setMaxLines(3);
            c.addView(d);
        }
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        Button up = smallButton("▲");
        up.setEnabled(idx > 0);
        up.setOnClickListener(v -> movePack(packs, idx, -1));
        Button down = smallButton("▼");
        down.setEnabled(idx < packs.size() - 1);
        down.setOnClickListener(v -> movePack(packs, idx, 1));
        Button info = smallButton("ⓘ");
        info.setOnClickListener(v -> packDetails(p));
        row.addView(up);
        row.addView(down);
        row.addView(info);
        if (!LOOSE.equals(p.key)) {
            Button del = smallButton("🗑");
            del.setOnClickListener(v -> confirmDeletePack(p));
            row.addView(del);
        }
        c.addView(row);
        return c;
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(TEXT);
        b.setBackground(rounded(CARD2, 20));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(52), dp(40));
        lp.setMargins(dp(4), dp(6), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void movePack(List<Pack> packs, int idx, int delta) {
        int to = idx + delta;
        if (to < 0 || to >= packs.size()) return;
        Collections.swap(packs, idx, to);
        List<String> order = new ArrayList<>();
        for (Pack p : packs) order.add(p.key);
        saveListSetting("mods_order", order);
        showTab(1);
    }

    private void packDetails(Pack p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.name).append("\n");
        if (p.version != null) sb.append(L("Version ", "Versi ")).append(p.version).append("\n");
        if (p.author != null) sb.append(L("Author ", "Pembuat ")).append(p.author).append("\n");
        sb.append(fmtSize(p.bytes)).append(" · ").append(p.files).append(L(" files\n", " file\n"));
        sb.append(L("Folder: ", "Folder: ")).append(p.dir.getAbsolutePath()).append("\n");
        if (p.description != null) sb.append("\n").append(p.description);
        new AlertDialog.Builder(this)
                .setMessage(sb.toString())
                .setPositiveButton(L("Close", "Tutup"), null)
                .setNeutralButton(p.url != null ? L("Open page", "Buka halaman") : null,
                        p.url != null ? (d, w) -> openUrl(p.url) : null)
                .show();
    }

    private void confirmDeletePack(Pack p) {
        new AlertDialog.Builder(this)
                .setTitle(L("Delete this mod?", "Hapus mod ini?"))
                .setMessage(p.name + L("\n\nThe folder and everything in it is deleted from "
                        + "the Mods folder. This cannot be undone.", "\n\nFolder dan seluruh "
                        + "isinya akan dihapus dari folder Mods. Tindakan ini tidak dapat "
                        + "dibatalkan."))
                .setNegativeButton(L("Cancel", "Batal"), null)
                .setPositiveButton(L("Delete", "Hapus"), (d, w) -> io.execute(() -> {
                    deleteTree(p.dir);
                    List<String> order = listSetting("mods_order");
                    order.remove(p.key);
                    saveListSetting("mods_order", order);
                    List<String> disabled = listSetting("mods_disabled");
                    disabled.remove(p.key);
                    saveListSetting("mods_disabled", disabled);
                    ui.post(() -> showTab(1));
                }))
                .show();
    }

    private static void deleteTree(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteTree(k);
        }
        f.delete();
    }

    // ------------------------------------------------------------- iso import

    // ISO files a player plausibly has lying around: the Download folders and
    // the storage roots - never inside the THPS4 folder itself (that is the
    // destination, and a file there is already imported)
    private List<File> findIsoFiles() {
        List<File> roots = new ArrayList<>();
        File internal = new File("/storage/emulated/0");
        roots.add(internal);
        roots.add(new File(internal, "Download"));
        roots.add(new File(internal, "Downloads"));
        File[] vols = new File("/storage").listFiles();
        if (vols != null)
            for (File v : vols) {
                if (v.getName().equals("emulated") || v.getName().equals("self")) continue;
                roots.add(v);
                roots.add(new File(v, "Download"));
                roots.add(new File(v, "Downloads"));
            }
        Map<String, File> byPath = new HashMap<>();
        String sharedCanon = null;
        try {
            sharedCanon = new File(SHARED).getCanonicalPath();
        } catch (Exception ignored) {
        }
        for (File root : roots) {
            File[] files = root.listFiles();
            if (files == null) continue;
            for (File f : files) {
                if (!f.isFile() || !f.getName().toLowerCase(Locale.US).endsWith(".iso")) continue;
                try {
                    String canon = f.getCanonicalPath();
                    if (sharedCanon != null && canon.startsWith(sharedCanon + File.separator)) continue;
                    byPath.put(canon, f);
                } catch (Exception ignored) {
                }
            }
        }
        List<File> out = new ArrayList<>(byPath.values());
        out.sort(Comparator.comparing(f -> f.getName().toLowerCase(Locale.US)));
        return out;
    }

    private void startIsoImport() {
        Toast.makeText(this, L("Looking for ISO files…", "Mencari file ISO…"), Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            List<File> found = findIsoFiles();
            ui.post(() -> {
                if (found.isEmpty()) {
                    pickIsoFile();
                    return;
                }
                List<String> labels = new ArrayList<>();
                for (File f : found)
                    labels.add(f.getName() + "  ·  " + fmtSize(f.length()) + "  ·  " + f.getParent());
                labels.add(L("Browse for a file…", "Cari file lain…"));
                new AlertDialog.Builder(this)
                        .setTitle(L("Move which ISO into the THPS4 folder?", "Pindahkan ISO yang mana ke folder THPS4?"))
                        .setItems(labels.toArray(new String[0]), (d, which) -> {
                            if (which < found.size()) prepareIsoMove(found.get(which), null, found.get(which).getName());
                            else pickIsoFile();
                        })
                        .setNegativeButton(L("Cancel", "Batal"), null)
                        .show();
            });
        });
    }

    private void pickIsoFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"application/x-iso9660-image", "application/octet-stream"});
        try {
            startActivityForResult(i, REQ_ISO);
        } catch (Exception e) {
            Toast.makeText(this, L("No file picker available", "Pemilih file tidak tersedia"), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleIsoPicked(Uri uri) {
        String name = displayName(uri);
        if (name == null || !name.toLowerCase(Locale.US).endsWith(".iso")) {
            Toast.makeText(this, L("That is not an .iso file", "Itu bukan file .iso"), Toast.LENGTH_LONG).show();
            return;
        }
        // picking the file that already sits in the THPS4 folder would move
        // it onto itself - refuse before anything is copied
        try {
            String docId = DocumentsContract.getDocumentId(uri);
            int colon = docId.indexOf(':');
            if (colon >= 0 && docId.substring(colon + 1).equals("THPS4/" + name)) {
                Toast.makeText(this, L("That ISO is already in the THPS4 folder", "ISO itu sudah ada di folder THPS4"), Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception ignored) {
        }
        prepareIsoMove(null, uri, name);
    }

    private void prepareIsoMove(File src, Uri uri, String name) {
        File dest = new File(new File(SHARED), name);
        if (src != null) {
            try {
                if (src.getCanonicalPath().equals(dest.getCanonicalPath())) {
                    Toast.makeText(this, L("That ISO is already in the THPS4 folder", "ISO itu sudah ada di folder THPS4"), Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        if (dest.exists()) {
            new AlertDialog.Builder(this)
                    .setTitle(L("Replace the existing ISO?", "Ganti ISO yang sudah ada?"))
                    .setMessage(name + "  ·  " + fmtSize(dest.length()))
                    .setPositiveButton(L("Replace", "Ganti"), (d, w) -> doIsoMove(src, uri, name))
                    .setNegativeButton(L("Cancel", "Batal"), null)
                    .show();
        } else {
            doIsoMove(src, uri, name);
        }
    }

    // the move itself: a rename when the source is a real file on the same
    // storage (instant), otherwise a copy into a temp file in the THPS4
    // folder, verified, renamed into place, and only then is the source
    // deleted - a failed copy never costs the player their only ISO
    private void doIsoMove(File src, Uri uri, String name) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(8));
        TextView label = text(L("Moving ISO…", "Memindahkan ISO…"), 15, TEXT);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        bar.setIndeterminate(true);
        box.addView(label);
        box.addView(bar);
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(L("Import ISO", "Impor ISO"))
                .setView(box)
                .setCancelable(false)
                .show();
        io.execute(() -> {
            File dir = new File(SHARED);
            File dest = new File(dir, name);
            boolean ok = false;
            boolean sourceGone = false;
            try {
                dir.mkdirs();
                if (src != null && !dest.exists() && src.renameTo(dest)) {
                    ok = true;
                    sourceGone = true;
                } else {
                    long total = -1;
                    InputStream in;
                    if (src != null) {
                        total = src.length();
                        in = new FileInputStream(src);
                    } else {
                        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                            if (c != null && c.moveToFirst()) {
                                int i = c.getColumnIndex(OpenableColumns.SIZE);
                                if (i >= 0 && !c.isNull(i)) total = c.getLong(i);
                            }
                        } catch (Exception ignored) {
                        }
                        in = getContentResolver().openInputStream(uri);
                    }
                    if (in == null) throw new Exception("source unreadable");
                    File tmp = new File(dir, name + ".importing");
                    long done = 0;
                    long lastUi = 0;
                    try (InputStream input = in; FileOutputStream fos = new FileOutputStream(tmp)) {
                        byte[] buf = new byte[1024 * 1024];
                        int n;
                        while ((n = input.read(buf)) > 0) {
                            fos.write(buf, 0, n);
                            done += n;
                            if (total > 0 && done - lastUi > 4_000_000L) {
                                lastUi = done;
                                final long dNow = done, tNow = total;
                                ui.post(() -> {
                                    bar.setIndeterminate(false);
                                    bar.setProgress((int) (1000L * dNow / tNow));
                                    label.setText(L("Moving ISO… ", "Memindahkan ISO… ")
                                            + fmtSize(dNow) + " / " + fmtSize(tNow));
                                });
                            }
                        }
                        fos.flush();
                    }
                    if (total > 0 && tmp.length() != total) {
                        tmp.delete();
                        throw new Exception("copy incomplete");
                    }
                    if (dest.exists() && !dest.delete()) {
                        tmp.delete();
                        throw new Exception("cannot replace");
                    }
                    if (!tmp.renameTo(dest)) {
                        tmp.delete();
                        throw new Exception("cannot finish");
                    }
                    ok = true;
                    if (src != null) {
                        sourceGone = src.delete();
                    } else {
                        try {
                            sourceGone = DocumentsContract.deleteDocument(getContentResolver(), uri);
                        } catch (Exception ignored) {
                            sourceGone = false;
                        }
                    }
                }
            } catch (Exception e) {
                ok = false;
            }
            final boolean okF = ok, goneF = sourceGone;
            ui.post(() -> {
                dlg.dismiss();
                if (okF) {
                    resolveGameDir();
                    Toast.makeText(this, goneF
                            ? L("ISO moved - the game is ready", "ISO dipindahkan - game siap dimainkan")
                            : L("ISO copied, but the original could not be deleted - you can delete it yourself",
                                "ISO tersalin, tapi file asal tidak bisa dihapus - silakan hapus sendiri"), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, L("Import failed", "Impor gagal"), Toast.LENGTH_LONG).show();
                }
                showTab(0);
            });
        });
    }

    // ------------------------------------------------------------- zip import

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null)
            return;
        if (requestCode == REQ_ISO) {
            handleIsoPicked(data.getData());
            return;
        }
        if (requestCode != REQ_ZIP)
            return;
        Uri uri = data.getData();
        if (modsDir == null) {
            Toast.makeText(this, L("Game not found - no Mods folder yet", "Game tidak ditemukan - belum ada folder Mods"), Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, L("Importing…", "Mengimpor…"), Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            String name = displayName(uri);
            if (name == null) name = "mod";
            if (name.toLowerCase(Locale.US).endsWith(".zip"))
                name = name.substring(0, name.length() - 4);
            name = name.replaceAll("[^A-Za-z0-9 _.-]", "_").trim();
            if (name.isEmpty()) name = "mod";
            File dest = new File(modsDir, name);
            for (int n = 2; dest.exists(); n++) dest = new File(modsDir, name + " " + n);
            final File destDir = dest;
            boolean ok = unzipInto(uri, destDir);
            ui.post(() -> {
                Toast.makeText(this, ok ? L("Imported ", "Berhasil mengimpor ") + destDir.getName()
                        : L("Import failed (not a valid zip?)", "Impor gagal (bukan zip yang valid?)"), Toast.LENGTH_LONG).show();
                showTab(1);
            });
        });
    }

    private String displayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean unzipInto(Uri uri, File dest) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ZipInputStream zip = new ZipInputStream(in)) {
            dest.mkdirs();
            String destPath = dest.getCanonicalPath();
            ZipEntry e;
            byte[] buf = new byte[64 * 1024];
            while ((e = zip.getNextEntry()) != null) {
                File out = new File(dest, e.getName());
                if (!out.getCanonicalPath().startsWith(destPath)) continue;  // zip slip
                if (e.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                out.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    int n;
                    while ((n = zip.read(buf)) > 0) fos.write(buf, 0, n);
                }
            }
            return true;
        } catch (Exception e) {
            deleteTree(dest);
            return false;
        }
    }

    // ----------------------------------------------------------------- music

    private static class Track {
        File file;
        String relKey;   // "Song.wma" loose, "Album/Song.wma" in an album
        long bytes, ms;
    }

    private static class Album {
        String name;
        final List<Track> tracks = new ArrayList<>();
        long bytes, ms;
    }

    private static class MusicLib {
        final List<Album> albums = new ArrayList<>();
        int tracks;
        long bytes;
    }

    // a song's length from its ASF header, the same fields the native
    // importer reads (native/soundtrack.cpp WmaDurationMs)
    private static long wmaDurationMs(File f) {
        byte[] buf = new byte[1 << 16];
        int n = 0;
        try (InputStream in = new FileInputStream(f)) {
            while (n < buf.length) {
                int r = in.read(buf, n, buf.length - n);
                if (r < 0) break;
                n += r;
            }
        } catch (Exception e) {
            return 0;
        }
        byte[] header = {0x30, 0x26, (byte) 0xB2, 0x75, (byte) 0x8E, 0x66, (byte) 0xCF, 0x11,
                (byte) 0xA6, (byte) 0xD9, 0x00, (byte) 0xAA, 0x00, 0x62, (byte) 0xCE, 0x6C};
        // ASF object GUIDs sit on disk in text order (A1DCAB8C-47A9-CF11-...)
        byte[] props = {(byte) 0xA1, (byte) 0xDC, (byte) 0xAB, (byte) 0x8C, 0x47, (byte) 0xA9,
                (byte) 0xCF, 0x11, (byte) 0x8E, (byte) 0xE4, 0x00, (byte) 0xC0, 0x0C, 0x20, 0x53, 0x65};
        if (n < 30 || !matches(buf, 0, header)) return 0;
        int pos = 30;
        while (pos + 24 <= n) {
            long size = u64le(buf, pos + 16);
            if (size < 24) break;
            if (matches(buf, pos, props) && pos + 24 + 64 <= n) {
                long playMs = u64le(buf, pos + 24 + 40) / 10000;
                long preroll = u64le(buf, pos + 24 + 56);
                return playMs > preroll ? playMs - preroll : playMs;
            }
            pos += (int) size;
        }
        return 0;
    }

    private static boolean matches(byte[] buf, int off, byte[] pat) {
        if (off + pat.length > buf.length) return false;
        for (int i = 0; i < pat.length; i++) if (buf[off + i] != pat[i]) return false;
        return true;
    }

    private static long u64le(byte[] buf, int off) {
        long v = 0;
        for (int i = 0; i < 8; i++) v |= (long) (buf[off + i] & 0xFF) << (8 * i);
        return v;
    }

    // the music source the game will use: 0 = the game's own soundtrack
    // machinery (WMA custom tracks), 1 = My Music, played by the port itself
    private int musicSource() {
        try {
            return Integer.parseInt(settings.getOrDefault("music_source", "0"));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private MusicLib scanMusic() {
        boolean mine = musicSource() == 1;
        MusicLib lib = new MusicLib();
        File music = new File(SHARED, "Music");
        if (!music.isDirectory()) return lib;
        List<File> loose = audioFiles(music, mine);
        if (!loose.isEmpty()) lib.albums.add(makeAlbum(L("Custom Soundtrack", "Soundtrack Kustom"), loose, ""));
        File[] kids = music.listFiles();
        List<File> subs = new ArrayList<>();
        if (kids != null)
            for (File k : kids) if (k.isDirectory()) subs.add(k);
        Collections.sort(subs, Comparator.comparing(f -> f.getName().toLowerCase(Locale.US)));
        for (File sub : subs) {
            List<File> songs = audioFiles(sub, mine);
            if (!songs.isEmpty())
                lib.albums.add(makeAlbum(sub.getName(), songs, sub.getName() + "/"));
        }
        for (Album a : lib.albums) {
            lib.tracks += a.tracks.size();
            lib.bytes += a.bytes;
        }
        return lib;
    }

    // the song files of one folder for the active source: the game's
    // soundtrack takes .wma only; My Music takes the formats the phone's
    // own codecs play (native/mymusic.cpp) and leaves .wma to the game
    private static List<File> audioFiles(File dir, boolean mine) {
        String[] gameExts = {".wma"};
        String[] mineExts = {".mp3", ".m4a", ".ogg", ".oga", ".opus", ".flac", ".wav"};
        String[] exts = mine ? mineExts : gameExts;
        List<File> out = new ArrayList<>();
        File[] kids = dir.listFiles();
        if (kids != null)
            for (File k : kids) {
                if (!k.isFile()) continue;
                String n = k.getName().toLowerCase(Locale.US);
                for (String e : exts) if (n.endsWith(e)) { out.add(k); break; }
            }
        Collections.sort(out, Comparator.comparing(f -> f.getName().toLowerCase(Locale.US)));
        return out;
    }

    // a song's length: WMA from its ASF header (the game's format), the
    // rest from the platform's metadata reader
    private static long trackDurationMs(File f) {
        if (f.getName().toLowerCase(Locale.US).endsWith(".wma")) return wmaDurationMs(f);
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(f.getAbsolutePath());
            String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return d == null ? 0 : Long.parseLong(d);
        } catch (Exception e) {
            return 0;
        } finally {
            try { r.release(); } catch (Exception ignored) { }
        }
    }

    private Album makeAlbum(String name, List<File> songs, String prefix) {
        Album a = new Album();
        a.name = name;
        for (File f : songs) {
            Track t = new Track();
            t.file = f;
            t.relKey = prefix + f.getName();
            t.bytes = f.length();
            t.ms = trackDurationMs(f);
            a.tracks.add(t);
            a.bytes += t.bytes;
            a.ms += t.ms;
        }
        return a;
    }

    private void showMusic() {
        boolean mine = musicSource() == 1;

        LinearLayout src = card();
        src.addView(text(L("Music source", "Sumber musik"), 18, TEXT));
        src.addView(text(L("The game's soundtrack, or your own songs played directly "
                + "by the port. Applies when the game starts.",
                "Soundtrack bawaan game, atau lagu kamu sendiri yang diputar langsung "
                + "oleh port ini. Berlaku saat game dimulai."), 14, MUTED));
        LinearLayout srow = new LinearLayout(this);
        srow.setOrientation(LinearLayout.HORIZONTAL);
        String[] snames = {L("Game soundtrack", "Soundtrack game"), L("My Music", "Musik Saya")};
        for (int i = 0; i < snames.length; i++) {
            final int idx = i;
            Button b = new Button(this);
            b.setText(snames[i]);
            b.setAllCaps(false);
            b.setTextSize(15);
            b.setTextColor(i == (mine ? 1 : 0) ? 0xFF1A1400 : TEXT);
            b.setBackground(rounded(i == (mine ? 1 : 0) ? ACCENT : CARD2, 12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.setMargins(dp(3), dp(8), dp(3), 0);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> {
                saveSetting("music_source", String.valueOf(idx), false);
                showTab(2);
            });
            srow.addView(b);
        }
        src.addView(srow);
        content.addView(src);

        LinearLayout info = card();
        info.addView(text(L("Custom tracks", "Lagu kustom"), 18, TEXT));
        if (mine) {
            info.addView(text(L("Your songs are played by the port itself, straight "
                    + "from the Music folder - no conversion. The game's own music "
                    + "steps aside automatically, and the volume follows the game's "
                    + "Music Volume slider. Loose songs form one album; each subfolder "
                    + "is an album of its own. Switched-off songs are skipped.",
                    "Lagu-lagu kamu diputar oleh port ini sendiri, langsung dari "
                    + "folder Music - tanpa konversi. Musik bawaan game menyingkir "
                    + "otomatis, dan volumenya mengikuti slider Music Volume di dalam "
                    + "game. Lagu lepas membentuk satu album; tiap subfolder adalah "
                    + "album tersendiri. Lagu yang dinonaktifkan dilewati."), 14, MUTED));
        } else {
            info.addView(text(L("Your songs play inside the game as a custom soundtrack: "
                    + "Sound Options → Soundtrack. Loose songs in the Music folder form one "
                    + "album; each subfolder is an album of its own, named after the folder. "
                    + "Switched-off songs are skipped when the game builds the soundtrack "
                    + "at start.", "Lagu-lagu kamu diputar di dalam game sebagai soundtrack "
                    + "kustom: Opsi Suara → Soundtrack. Lagu lepas di folder Music membentuk "
                    + "satu album; tiap subfolder adalah album tersendiri, dinamai sesuai "
                    + "foldernya. Lagu yang dinonaktifkan dilewati saat game menyusun "
                    + "soundtrack ketika dimulai."), 14, MUTED));
        }
        info.addView(text(L("Music folder: ", "Folder musik: ") + SHARED + "/Music", 13, MUTED));
        content.addView(info);

        LinearLayout limits = card();
        limits.addView(text(L("Limits", "Batas"), 18, TEXT));
        if (mine) {
            limits.addView(text(L("•  Formats: MP3, M4A/AAC, OGG, Opus, FLAC, WAV - "
                    + "whatever the phone itself can play (WMA stays with the game "
                    + "soundtrack source)", "•  Format: MP3, M4A/AAC, OGG, Opus, FLAC, "
                    + "WAV - apa pun yang bisa diputar HP itu sendiri (WMA tetap di "
                    + "sumber soundtrack game)"), 14, MUTED));
            limits.addView(text(L("•  No per-song size or duration limit",
                    "•  Tidak ada batas ukuran atau durasi per lagu"), 14, MUTED));
        } else {
            limits.addView(text(L("•  Format: WMA only - the game's own decoder plays them "
                    + "(44.1 kHz stereo is what the original Xbox dashboard used)",
                    "•  Format: WMA saja - decoder bawaan game yang memutarnya "
                    + "(44,1 kHz stereo adalah yang dipakai dashboard Xbox asli)"), 14, MUTED));
            limits.addView(text(L("•  Max size: 64 MB per song", "•  Ukuran maksimal: 64 MB per lagu"), 14, MUTED));
            limits.addView(text(L("•  Max amount: 500 songs per album, 100 albums", "•  Jumlah maksimal: 500 lagu per album, 100 album"), 14, MUTED));
            limits.addView(text(L("•  Duration: read from each song file - there is no "
                    + "per-song duration limit", "•  Durasi: dibaca dari file lagu - "
                    + "tidak ada batas durasi per lagu"), 14, MUTED));
        }
        content.addView(limits);

        TextView loading = text(L("Loading songs…", "Memuat lagu…"), 15, MUTED);
        content.addView(loading);
        io.execute(() -> {
            MusicLib lib = scanMusic();
            Set<String> off = new HashSet<>(listSetting("music_disabled"));
            ui.post(() -> {
                if (tab != 2) return;
                content.removeView(loading);
                if (lib.albums.isEmpty()) {
                    content.addView(text(mine
                            ? L("No songs yet. Copy MP3 (or M4A, OGG, FLAC, WAV) files "
                            + "into the Music folder above and they will show up here.",
                            "Belum ada lagu. Salin file MP3 (atau M4A, OGG, FLAC, WAV) "
                            + "ke folder Music di atas dan lagu akan muncul di sini.")
                            : L("No songs yet. Copy .wma files into the Music "
                            + "folder above and they will show up here.", "Belum ada lagu. "
                            + "Salin file .wma ke folder Music di atas dan lagu akan "
                            + "muncul di sini."), 14, MUTED));
                    return;
                }
                for (Album a : lib.albums) {
                    LinearLayout head = card();
                    head.addView(text(a.name, 17, ACCENT));
                    head.addView(text(a.tracks.size() + L(" songs · ", " lagu · ") + fmtDuration(a.ms)
                            + " · " + fmtSize(a.bytes), 13, MUTED));
                    content.addView(head);
                    for (Track t : a.tracks) content.addView(trackRow(t, off));
                }
            });
        });
    }

    private LinearLayout trackRow(Track t, Set<String> off) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(2), dp(8), dp(2));
        CheckBox cb = new CheckBox(this);
        cb.setChecked(!off.contains(t.relKey));
        cb.setOnCheckedChangeListener((v, checked) -> {
            List<String> disabled = listSetting("music_disabled");
            if (checked) disabled.remove(t.relKey);
            else if (!disabled.contains(t.relKey)) disabled.add(t.relKey);
            saveListSetting("music_disabled", disabled);
        });
        row.addView(cb);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        String name = t.file.getName();
        int dot = name.lastIndexOf('.');
        titles.addView(text(dot > 0 ? name.substring(0, dot) : name, 15, TEXT));
        boolean over = musicSource() == 0 && t.bytes > 64L * 1024 * 1024;
        titles.addView(text(fmtDuration(t.ms) + "  ·  " + fmtSize(t.bytes)
                + (over ? L("  ·  over the 64 MB limit - skipped", "  ·  melebihi batas 64 MB - dilewati") : ""), 13,
                over ? BAD : MUTED));
        row.addView(titles);
        return row;
    }
}
