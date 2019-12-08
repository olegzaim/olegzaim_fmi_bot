package org.fmibot.maven.app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sun.activation.registries.LogSupport.log;

public class FmiBotClass extends TelegramLongPollingBot {

    final String specialityName = "Софтуерно инженерство";
    final String fmiLessonsCommand = "/fmilessonsnews";
    final String elementClassName = "contentx";

    public void onUpdateReceived(Update update) {
        System.out.println(update.getMessage().getText());

        String command = update.getMessage().getText();
        SendMessage message = new SendMessage();

        if (command.equals(fmiLessonsCommand)) {
            Document doc = null;
            String textNews = "";

            try {
                doc = Jsoup.connect("http://fmi-plovdiv.org/news.jsp?ln=1&newsId=1753&newsPageNumber=1").get();
            } catch (IOException e) {
                System.out.println("IO exception");
                e.printStackTrace();
            }

            Elements contentTextElementsDiv = doc.getElementsByClass(elementClassName).select("div");
            Elements contentTextElementsP = doc.getElementsByClass(elementClassName).select("p");

            String concatText = "";
            String textNewsInDivTag = splitAndConcatText(contentTextElementsDiv, concatText);
            String textNewsInPTag = splitAndConcatText(contentTextElementsP, concatText);
            textNews = textNewsInDivTag.concat(textNewsInPTag);
            //4000 byte is a limit of telegram message
            if (textNews.length() > 4000) {
                textNews = textNews.substring(0, 4000);
            }
            if(textNews.isEmpty()){
                textNews = "No actual news for speciality: "+specialityName;
            }

            message.setText(textNews);
            System.out.println(message);
        }

        message.setChatId(update.getMessage().getChatId());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }


    }

    public String splitAndConcatText(Elements contentTextElements, String concatText) {
        int counter = 0;
        try {
            for (Element e :
                    contentTextElements) {
                String text = e.text();
                if (e.text().length() == 0) {
                    continue;
                }
                if (!e.text().contains(specialityName) || !e.text().contains(specialityName.toUpperCase())) {
                    continue;
                }
                if (!validateDate(text)) {
                    continue;
                }


                String tempString = concatText;

                counter++;
                tempString += "\n" + counter + "." + text;
                if (tempString.length() > 4000) {
                    return concatText;
                }

                concatText = tempString;

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return concatText;
    }

    private boolean validateDate(String text) {
        Date lessonDate = null;
        Date now = null;
        ArrayList<String> dates = new ArrayList<>();
        int iend = text.indexOf("г.");
        Pattern pattern = Pattern.compile("[^г.]*г.");
        Matcher matcher = pattern.matcher(text);


        boolean flag = false;
        if (matcher.find()) {
            for (int i = 0; i < text.length(); i++) {
                if (!text.contains("г.")) {
                    flag = false;
                    break;
                }
                String otherDatesInText = text.substring(0, text.indexOf("г."));
                StringBuilder subString = new StringBuilder(otherDatesInText);
                subString.reverse();
                otherDatesInText = subString.toString();
                if (otherDatesInText.length() < 14) {
                    flag = false;
                    continue;
                }
                String removableText = otherDatesInText.substring(0, 14);
                removableText = new StringBuilder(removableText).reverse().toString();
                text = text.replace(removableText, "");
                text = text.substring(0, text.indexOf("г.")) + text.substring(text.indexOf("г.") + 2);
                otherDatesInText = otherDatesInText.substring(0, 11);
                if (!otherDatesInText.matches("^[А-Яа-я].*$")) {
                    dates.add(new StringBuilder(otherDatesInText).reverse().toString());
                }

            }

            for (String dateString : dates) {
                if (dateString == null || dateString.matches("^[А-Яа-я].*$")) {
                    flag = false;
                    continue;
                }
                System.out.println(dateString);
                try {
                    DateFormat f = new SimpleDateFormat("dd.MM.yyyy");
                    String dateNow = f.format(new Date());
                    now = f.parse(dateNow);
                    lessonDate = f.parse(dateString);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    flag = false;
                    System.out.println("Format exception!");
                    continue;
                }
                if (lessonDate.before(now)) {
                    System.out.println("Skip date: " + lessonDate);
                    flag = false;
                    continue;
                } else {
                    System.out.println("LessonDate: " + lessonDate);
                }
                flag = true;
                break;
            }
        }
        return flag;
    }

    public String getBotUsername() {
        return "fmi_bot";
    }

    public String getBotToken() {
        return "930011493:AAEmtUC5a0Ncc14GIREYmYt0bmwol-u8uwI";
    }
}
