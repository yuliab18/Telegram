package org.campus;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.*;
import com.pengrad.telegrambot.request.SendMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static final String BOT_TOKEN = "7162043662:AAHptbQRb99QhkA5y5_CrFhM3sI4F6AFByg";
    public static Map<Long, TGUser> users = new HashMap<>();
    public static TelegramBot telegramBot = new TelegramBot(BOT_TOKEN);
    public static LogService logService = new LogService();

    public static void main(String[] args) {
        System.out.println("Hello world!");


        telegramBot.setUpdatesListener(new UpdatesListener() {
            @Override
            public int process(List<Update> list) {
                for (Update update : list)
                    newMessageFromUser(update);
                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            }
        });
    }

    public static void newMessageFromUser(Update update) {
        // Logs
        logService.log(update);
        // Приветствие
        Long userId = update.message().from().id();
        if (startMessage(userId)) return;


        // питання
        TGUser user = users.get(userId);
        if (questionMessage(userId, user, update)) return;

        // Результат
        resultMessage(userId, user);
    }


    public static Boolean startMessage(Long userId) {
        if (users.containsKey(userId)) {
            return false;
        }
        users.put(userId, new TGUser(userId));

        SendMessage sendMessage = new SendMessage(userId, Texts.HELLO_MESSAGE);
        sendMessage.parseMode(ParseMode.Markdown);

        Keyboard replyKeyboardMarkup = new ReplyKeyboardMarkup(Texts.HELLO_MESSAGE_BUTTON)
                .oneTimeKeyboard(true)
                .resizeKeyboard(true)
                .selective(true);

        sendMessage.replyMarkup(replyKeyboardMarkup);

        telegramBot.execute(sendMessage);
        return true;
    }


    public static Boolean questionMessage(Long userId, TGUser user, Update update) {
        for (Question question : user.getQuestions()) {
            if (question.getSendQuestionToUser() == true) {
                if (question.getHasAnswer() == false) {
                    question.setHasAnswer(true);
                    for (Answer answer : question.getAnswers()) {
                        if (answer.getText().equals(update.message().text())) {
                            if (answer.getLanguageType().equals(LanguageType.QA))
                                user.setQaPoint(user.getQaPoint() + 1);
                            if (answer.getLanguageType().equals(LanguageType.BACKEND))
                                user.setBackendPoint(user.getBackendPoint() + 1);
                            if (answer.getLanguageType().equals(LanguageType.FRONTEND))
                                user.setFrontedPoint(user.getFrontedPoint() + 1);
                        }
                    }
                }
            }
            if (question.getSendQuestionToUser() == false) {
                question.setSendQuestionToUser(true);

                SendMessage sendMessage = new SendMessage(userId, question.getText());
                sendMessage.parseMode(ParseMode.Markdown);

                String[][] keyButtons = new String[4][1];
                for (int i = 0; i < question.getAnswers().size(); i++) {
                    keyButtons[i][0] = question.getAnswers().get(i).getText();
                }

                Keyboard replyKeyboardMarkup = new ReplyKeyboardMarkup(keyButtons)
                        .oneTimeKeyboard(true)
                        .resizeKeyboard(true)
                        .selective(true);

                sendMessage.replyMarkup(replyKeyboardMarkup);
                telegramBot.execute(sendMessage);
                return true;
            }
        }
        return false;
    }


    public static void resultMessage(Long userId, TGUser user) {

        telegramBot.execute(new SendMessage(userId, Texts.RESULT_MESSAGE));

        String resultTest = "";
        if (user.getBackendPoint() >= user.getFrontedPoint() && user.getBackendPoint() >= user.getQaPoint()) {
            resultTest = Texts.RESULT_MESSAGE_JAVA;
        }
        if (user.getFrontedPoint() >= user.getBackendPoint() && user.getFrontedPoint() >= user.getQaPoint()) {
            resultTest = Texts.RESULT_MESSAGE_FRONTEND;
        }
        if (user.getQaPoint() >= user.getFrontedPoint() && user.getQaPoint() >= user.getBackendPoint()) {
            resultTest = Texts.RESULT_MESSAGE_QA;
        }

        SendMessage resultMessage = new SendMessage(userId, resultTest);
        resultMessage.parseMode(ParseMode.Markdown);
        telegramBot.execute(resultMessage);


        SendMessage advice = new SendMessage(userId, Texts.RESULT_MESSAGE_ADVICE);


        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton b = new InlineKeyboardButton(Texts.RESULT_MESSAGE_SAME_BOT);
        b.url(Texts.RESULT_MESSAGE_LINK);
        markup.addRow(b);

        advice.parseMode(ParseMode.Markdown);
        advice.replyMarkup(markup);
        telegramBot.execute(advice);

        SendMessage subscribe = new SendMessage(userId, Texts.RESULT_MESSAGE_BOT_NAME);
        Keyboard replyKeyboardMarkup = new ReplyKeyboardMarkup(Texts.RESULT_MESSAGE_RESTART)
                .oneTimeKeyboard(true)
                .resizeKeyboard(true)
                .selective(true);
        subscribe.replyMarkup(replyKeyboardMarkup);
        telegramBot.execute(subscribe);

        users.put(userId, new TGUser(userId));
    }
}
