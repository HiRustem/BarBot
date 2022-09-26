package com.example.Bar.services;

import com.example.Bar.configs.BotConfig;
import com.example.Bar.models.Position;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



@Component
public class TelegramBotService extends TelegramLongPollingBot {
    private static final String PATH = "list.json";
    private static final String HELLO = "Здравствуйте\nООО Вольфганза\nБирпоинт\n";

    private HashMap<String, List<Position>> checkList = new HashMap<>();

    private String providerName;
    private String positionName;

    final BotConfig config;

    public TelegramBotService(BotConfig config) {
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                sendStartMessage(chatId);
            } else if (messageText.matches("\\d+") && providerName != null && positionName != null) {
                setQuantity(chatId, Integer.parseInt(messageText), PATH, providerName, positionName);
                showPositions(chatId,"Список позиций " + providerName, PATH, providerName);
            } else if (messageText.equals("/form")) {
                createForm(chatId);
                providerName = "";
                positionName = "";
                checkList = new HashMap<>();
            }

        } else if (update.hasCallbackQuery()) {
            String callData = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callData.matches("show_positions/[А-я\\s]+")) {
                String[] split = callData.split("/");
                providerName = split[1];

                showPositions(chatId, "Список позиций " + providerName, PATH, providerName);

            } else if (callData.matches("set_quantity/[А-я\\s\\d]+")) {
                String[] split = callData.split("/");
                positionName = split[1];

                sendMessage(chatId, "Укажите количество для " + positionName + " :");
            } else if (callData.equals("positions_back")) {
                sendStartMessage(chatId);
            }
        }
    }

    private void sendStartMessage(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Список поставщиков:");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        try {
            Reader reader = Files.newBufferedReader(Paths.get(TelegramBotService.PATH));

            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode parser = objectMapper.readTree(reader);

            for (JsonNode provider : parser.path("providers")) {
                String providerName = provider.path("name").asText();

                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setText(providerName);
                inlineKeyboardButton.setCallbackData("show_positions/" + providerName);

                rowInline.add(inlineKeyboardButton);
            }

            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        rowsInline.add(rowInline);

        markupInline.setKeyboard(rowsInline);

        sendMessage.setReplyMarkup(markupInline);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void showPositions(Long chatId, String text, String path, String providerName) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        try {
            Reader reader = Files.newBufferedReader(Paths.get(path));

            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode parser = objectMapper.readTree(reader);

            for (JsonNode provider : parser.path("providers")) {
                String name = provider.path("name").asText();

                if (name.equals(providerName)) {
                    for (JsonNode position : provider.path("positions")) {
                        String positionName = position.get("name").asText();

                        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                        inlineKeyboardButton.setText(positionName);
                        inlineKeyboardButton.setCallbackData("set_quantity/" + positionName);

                        rowInline.add(inlineKeyboardButton);
                    }
                }
            }

            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText("Назад");
        inlineKeyboardButton.setCallbackData("positions_back");

        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();

        rowInline2.add(inlineKeyboardButton);

        rowsInline.add(rowInline);
        rowsInline.add(rowInline2);

        markupInline.setKeyboard(rowsInline);

        sendMessage.setReplyMarkup(markupInline);


        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void setQuantity(Long chatId, Integer quantity, String path, String providerName, String positionName) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        try {
            Reader reader = Files.newBufferedReader(Paths.get(path));

            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode parser = objectMapper.readTree(reader);

            for (JsonNode provider : parser.path("providers")) {
                String name = provider.path("name").asText();

                if (name.equals(providerName)) {
                    for (JsonNode position : provider.path("positions")) {
                        String posName = position.path("name").asText();

                        if (posName.equals(positionName)) {
                            Position position1 = new Position(posName, quantity, position.path("type").asText());
                            if (checkList.containsKey(providerName)) {
                                List<Position> positions = new ArrayList<>(checkList.get(providerName).stream().toList());
                                positions.add(position1);
                                checkList.put(providerName, positions);
                            } else {
                                checkList.put(providerName, List.of(position1));
                            }
                        }
                    }
                }
            }

            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        String result = "Позиция добавлена";

        sendMessage.setText(result);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void createForm(Long chatId) {
        checkList.forEach((k, v) -> {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(HELLO);

            for (Position position : v) {
                stringBuilder.append(position.getName()).append(" - ").append(position.getQuantity()).append(position.getType()).append(System.lineSeparator());
            }
            sendMessage(chatId, stringBuilder.toString());
        });
    }


    private void sendMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }


}
