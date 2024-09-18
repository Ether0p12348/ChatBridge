package com.ethan.chatbridge.events;

import com.ethan.chatbridge.ChatBridge;
import com.ethan.chatbridge.EndUserError;
import com.ethan.chatbridge.Payload;
import com.ethan.chatbridge.TranslateType;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MessageInteraction extends ListenerAdapter {
    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent e) {
        super.onMessageContextInteraction(e);

        switch (e.getName()) {
            case "Secret Translation":
            case "Public Translation":
                boolean isPublic = e.getName().equals("Public Translation");
                e.deferReply().setEphemeral(!isPublic).queue();

                Object reply = translateMessage(e, e.getUserLocale().getLocale());

                if (reply != null) {
                    if (reply instanceof String) {
                        e.getHook().editOriginal((String) reply).queue();
                    } else {
                        EndUserError err = (EndUserError) reply;
                        if (isPublic) {
                            e.getHook().deleteOriginal().queue();
                            e.reply(err.getLocaleMessages().get(e.getUserLocale())).setEphemeral(true).queue();
                        } else {
                            e.getHook().editOriginal(err.getLocaleMessages().get(e.getUserLocale())).queue();
                        }
                    }
                }
                break;
            default:
                String msg;

                switch (e.getUserLocale()) {
                    case DiscordLocale.BULGARIAN:
                        msg = "Това взаимодействие беше премахнато от разработчика";
                        break;
                    case DiscordLocale.CHINESE_CHINA:
                        msg = "此交互已被开发者移除";
                        break;
                    case DiscordLocale.CHINESE_TAIWAN:
                        msg = "此互動已被開發者移除";
                        break;
                    case DiscordLocale.CROATIAN:
                        msg = "Ovu interakciju je uklonio programer";
                        break;
                    case DiscordLocale.CZECH:
                        msg = "Tato interakce byla vývojářem odstraněna";
                        break;
                    case DiscordLocale.DANISH:
                        msg = "Denne interaktion er blevet fjernet af udvikleren";
                        break;
                    case DiscordLocale.DUTCH:
                        msg = "Deze interactie is door de ontwikkelaar verwijderd";
                        break;
                    case DiscordLocale.ENGLISH_UK:
                        msg = "This interaction has been removed by the developer";
                        break;
                    case DiscordLocale.ENGLISH_US:
                        msg = "This interaction has been removed by the developer.";
                        break;
                    case DiscordLocale.FINNISH:
                        msg = "Tämä vuorovaikutus on poistettu kehittäjän toimesta";
                        break;
                    case DiscordLocale.FRENCH:
                        msg = "Cette interaction a été supprimée par le développeur";
                        break;
                    case DiscordLocale.GERMAN:
                        msg = "Diese Interaktion wurde vom Entwickler entfernt";
                        break;
                    case DiscordLocale.GREEK:
                        msg = "Αυτή η αλληλεπίδραση έχει αφαιρεθεί από τον προγραμματιστή";
                        break;
                    case DiscordLocale.HINDI:
                        msg = "इस इंटरैक्शन को डेवलपर द्वारा हटा दिया गया है";
                        break;
                    case DiscordLocale.HUNGARIAN:
                        msg = "Ezt az interakciót a fejlesztő eltávolította";
                        break;
                    case DiscordLocale.INDONESIAN:
                        msg = "Interaksi ini telah dihapus oleh pengembang";
                        break;
                    case DiscordLocale.ITALIAN:
                        msg = "Questa interazione è stata rimossa dallo sviluppatore";
                        break;
                    case DiscordLocale.JAPANESE:
                        msg = "このインタラクションは開発者によって削除されました";
                        break;
                    case DiscordLocale.KOREAN:
                        msg = "이 상호작용은 개발자에 의해 제거되었습니다";
                        break;
                    case DiscordLocale.LITHUANIAN:
                        msg = "Šią sąveiką pašalino kūrėjas";
                        break;
                    case DiscordLocale.NORWEGIAN:
                        msg = "Denne interaksjonen har blitt fjernet av utvikleren";
                        break;
                    case DiscordLocale.POLISH:
                        msg = "Ta interakcja została usunięta przez programistę";
                        break;
                    case DiscordLocale.PORTUGUESE_BRAZILIAN:
                        msg = "Esta interação foi removida pelo desenvolvedor";
                        break;
                    case DiscordLocale.ROMANIAN_ROMANIA:
                        msg = "Această interacțiune a fost eliminată de către dezvoltator";
                        break;
                    case DiscordLocale.RUSSIAN:
                        msg = "Это взаимодействие было удалено разработчиком";
                        break;
                    case DiscordLocale.SPANISH:
                        msg = "Esta interacción ha sido eliminada por el desarrollador";
                        break;
                    case DiscordLocale.SPANISH_LATAM:
                        msg = "Esta interacción ha sido eliminada por el desarrollador";
                        break;
                    case DiscordLocale.SWEDISH:
                        msg = "Den här interaktionen har tagits bort av utvecklaren";
                        break;
                    case DiscordLocale.THAI:
                        msg = "การโต้ตอบนี้ถูกลบโดยนักพัฒนา";
                        break;
                    case DiscordLocale.TURKISH:
                        msg = "Bu etkileşim geliştirici tarafından kaldırıldı";
                        break;
                    case DiscordLocale.UKRAINIAN:
                        msg = "Цю взаємодію було видалено розробником";
                        break;
                    case DiscordLocale.VIETNAMESE:
                        msg = "Tương tác này đã bị nhà phát triển gỡ bỏ";
                        break;
                    default:
                        msg = "This interaction has been removed by the developer.";
                        break;
                }

                e.reply(msg).setEphemeral(true).queue();
                break;
        }
    }

    public Object translateMessage (@NotNull MessageContextInteractionEvent event, @NotNull String targetLanguage) {
        Payload payload = new Payload(null, TranslateType.DECORATED.getSystemPrompt(), "(" + targetLanguage + ")" + event.getTarget().getContentRaw(), 5000);
        try {
            payload.queue();
            return event.getTarget().getJumpUrl() + ": " + payload.getResult();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            Map<DiscordLocale, String> localeMessages = new HashMap<>();

            String discordInvite = ChatBridge.secret.get("discord", "discordInvite");
            localeMessages.put(DiscordLocale.BULGARIAN, "Неуспех при превеждането на съобщението. Моля, докладвайте този инцидент на официалния сървър в Discord за ChatBridge: " + discordInvite);
            localeMessages.put(DiscordLocale.CHINESE_CHINA, "消息翻译失败。请将此事件报告至官方的 ChatBridge Discord 服务器：" + discordInvite);
            localeMessages.put(DiscordLocale.CHINESE_TAIWAN, "訊息翻譯失敗。請將此事件回報至官方的 ChatBridge Discord 伺服器：" + discordInvite);
            localeMessages.put(DiscordLocale.CROATIAN, "Neuspjelo prevođenje poruke. Molimo prijavite ovaj incident na službenom ChatBridge Discord poslužitelju: " + discordInvite);
            localeMessages.put(DiscordLocale.CZECH, "Nepodařilo se přeložit zprávu. Nahlaste tento incident na oficiálním Discord serveru ChatBridge: " + discordInvite);
            localeMessages.put(DiscordLocale.DANISH, "Kunne ikke oversætte beskeden. Rapporter venligst denne hændelse til den officielle ChatBridge Discord-server: " + discordInvite);
            localeMessages.put(DiscordLocale.DUTCH, "Het vertalen van het bericht is mislukt. Meld dit voorval alstublieft op de officiële ChatBridge Discord Server: " + discordInvite);
            localeMessages.put(DiscordLocale.ENGLISH_UK, "Failed to translate the message. Please report this incident to the official ChatBridge Discord Server: " + discordInvite);
            localeMessages.put(DiscordLocale.ENGLISH_US, "Failed to translate the message. Please report this incident to the official ChatBridge Discord Server: " + discordInvite);
            localeMessages.put(DiscordLocale.FINNISH, "Viestin kääntäminen epäonnistui. Ilmoita tästä tapauksesta viralliselle ChatBridge Discord-palvelimelle: " + discordInvite);
            localeMessages.put(DiscordLocale.FRENCH, "Échec de la traduction du message. Veuillez signaler cet incident au serveur Discord officiel de ChatBridge: " + discordInvite);
            localeMessages.put(DiscordLocale.GERMAN, "Die Nachricht konnte nicht übersetzt werden. Bitte melden Sie diesen Vorfall dem offiziellen ChatBridge Discord-Server: " + discordInvite);
            localeMessages.put(DiscordLocale.GREEK, "Αποτυχία μετάφρασης του μηνύματος. Παρακαλώ αναφέρετε αυτό το περιστατικό στον επίσημο διακομιστή Discord του ChatBridge: " + discordInvite);
            localeMessages.put(DiscordLocale.HINDI, "संदेश का अनुवाद करने में विफल। कृपया इस घटना की रिपोर्ट आधिकारिक ChatBridge Discord सर्वर पर करें: " + discordInvite);
            localeMessages.put(DiscordLocale.HUNGARIAN, "Nem sikerült lefordítani az üzenetet. Kérjük, jelentse ezt az esetet a hivatalos ChatBridge Discord szerveren: " + discordInvite);
            localeMessages.put(DiscordLocale.INDONESIAN, "Gagal menerjemahkan pesan. Harap laporkan insiden ini ke Server Discord resmi ChatBridge: " + discordInvite);
            localeMessages.put(DiscordLocale.ITALIAN, "Impossibile tradurre il messaggio. Si prega di segnalare questo incidente al server ufficiale di ChatBridge su Discord: " + discordInvite);
            localeMessages.put(DiscordLocale.JAPANESE, "メッセージの翻訳に失敗しました。このインシデントを公式 ChatBridge Discord サーバーに報告してください: " + discordInvite);
            localeMessages.put(DiscordLocale.KOREAN, "메시지 번역에 실패했습니다. 이 사건을 공식 ChatBridge Discord 서버에 보고해 주세요: " + discordInvite);
            localeMessages.put(DiscordLocale.LITHUANIAN, "Nepavyko išversti žinutės. Prašome pranešti apie šį incidentą oficialiame ChatBridge Discord serveryje: " + discordInvite);
            localeMessages.put(DiscordLocale.NORWEGIAN, "Kunne ikke oversette meldingen. Vennligst rapporter denne hendelsen til den offisielle ChatBridge Discord-serveren: " + discordInvite);
            localeMessages.put(DiscordLocale.POLISH, "Nie udało się przetłumaczyć wiadomości. Proszę zgłosić ten incydent na oficjalnym serwerze Discord ChatBridge: " + discordInvite);
            localeMessages.put(DiscordLocale.PORTUGUESE_BRAZILIAN, "Falha ao traduzir a mensagem. Por favor, reporte este incidente ao servidor oficial do ChatBridge no Discord: " + discordInvite);
            localeMessages.put(DiscordLocale.ROMANIAN_ROMANIA, "Nu s-a reușit traducerea mesajului. Vă rugăm să raportați acest incident pe serverul oficial ChatBridge Discord: " + discordInvite);
            localeMessages.put(DiscordLocale.RUSSIAN, "Не удалось перевести сообщение. Пожалуйста, сообщите об этом инциденте на официальном сервере ChatBridge в Discord: " + discordInvite);
            localeMessages.put(DiscordLocale.SPANISH, "No se pudo traducir el mensaje. Informe de este incidente en el servidor oficial de Discord de ChatBridge: " + discordInvite);
            localeMessages.put(DiscordLocale.SPANISH_LATAM, "No se pudo traducir el mensaje. Por favor, informe de este incidente en el servidor de Discord oficial de ChatBridge: " + discordInvite);
            localeMessages.put(DiscordLocale.SWEDISH, "Misslyckades med att översätta meddelandet. Vänligen rapportera denna incident till den officiella ChatBridge Discord-servern: " + discordInvite);
            localeMessages.put(DiscordLocale.THAI, "ไม่สามารถแปลข้อความนี้ได้ โปรดแจ้งเหตุการณ์นี้ให้เซิร์ฟเวอร์ Discord อย่างเป็นทางการของ ChatBridge ทราบ: " + discordInvite);
            localeMessages.put(DiscordLocale.TURKISH, "Mesaj tercüme edilemedi. Lütfen bu olayı resmi ChatBridge Discord Sunucusuna bildirin: " + discordInvite);
            localeMessages.put(DiscordLocale.UKRAINIAN, "Не вдалося перекласти повідомлення. Будь ласка, повідомте про цей випадок на офіційному сервері ChatBridge у Discord: " + discordInvite);
            localeMessages.put(DiscordLocale.VIETNAMESE, "Không thể dịch tin nhắn. Vui lòng báo cáo sự cố này cho Máy chủ Discord chính thức của ChatBridge: " + discordInvite);

            return new EndUserError(e, localeMessages);
        }
    }
}
