package com.example.manicurebot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static java.awt.SystemColor.text;


@Component
public class MyTelegramBotApi {

    private final String botToken;
    private final String baseUrl;

    private long offset;
    private GoogleCloudStorageUploader storageUploader;

    private final FeedbackService feedbackService;

    private final ObjectMapper objectMapper = new ObjectMapper();


    private Storage storage;
    private final String bucketName = "telegram_manicure_bot";

    private final String photoUrl1 = "https://storage.googleapis.com/telegram_manicure_bot/photo_2023-11-21_19-58-02.jpg";
    private final String photoUrl2 = "https://storage.googleapis.com/telegram_manicure_bot/photo_2023-11-21_19-58-04.jpg";
    private final String photoUrl3 = "https://storage.googleapis.com/telegram_manicure_bot/photo_2023-11-21_19-58-07.jpg";
    private final String photoUrl4 = "https://storage.googleapis.com/telegram_manicure_bot/photo_2023-11-21_19-58-06.jpg";

    @Autowired
    private AppointmentService appointmentService;


    @Autowired
    public MyTelegramBotApi(@Value("${telegram.bot.token}") String botToken,
                            GoogleCloudStorageUploader storageUploader, FeedbackService feedbackService) {
        this.botToken = botToken;
        this.baseUrl = "https://api.telegram.org/bot" + botToken;
        this.feedbackService = feedbackService;
        this.offset = 0;

        this.storageUploader = storageUploader;
        initializeStorage();
    }



    public void downloadPhoto(String photoUrl, String fileName, long chatId) {
        try {
            URL url = new URL(photoUrl);

            try (InputStream inputStream = url.openStream()) {

                storageUploader.uploadFile(inputStream, fileName);
            }

            System.out.println("Фотография успешно загружена в Google Cloud Storage.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Не удалось загрузить фотографию в Google Cloud Storage.");
        }
    }

    private ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("/start"));
        row.add(new KeyboardButton("/help"));
        row.add(new KeyboardButton("/photos"));
        row.add(new KeyboardButton("/make_an_appointment"));
        row.add(new KeyboardButton("/price"));
        row.add(new KeyboardButton("/feedback"));
        row.add(new KeyboardButton("/write_feedback"));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    @Scheduled(fixedRate = 1000)
    public void scheduledProcessUpdates() {
        processUpdates();
    }

    public void processUpdates() {
        String url = baseUrl + "/getUpdates?offset=" + offset + "&timeout=30";

        try {
            URL urlObject = new URL(url);

            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
            connection.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                if (connection.getResponseCode() == 200) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(response.toString());

                    if (rootNode.has("result")) {
                        for (JsonNode updateNode : rootNode.get("result")) {
                            long updateId = updateNode.get("update_id").asLong();
                            offset = Math.max(offset, updateId + 1);
                            long chatId = updateNode.get("message").get("chat").get("id").asLong();
                            String text = updateNode.get("message").get("text").asText();

                            System.out.println("Received update: " + text);

                            if ("/start".equals(text)) {
                                System.out.println("Received /start command");
                                sendStartMessage(chatId);
                            } else if ("/help".equals(text)) {
                                System.out.println("Received /help command");
                                sendHelpMessage(chatId);
                            } else if ("/photos".equals(text)) {
                                System.out.println("Received /photos command");
                                sendPhotos(chatId);
                            } else if ("/price".equals(text)) {
                                System.out.println("Received /price command");
                                sendPriceMessage(chatId);
                            } else if ("/feedback".equals(text)) {
                                System.out.println("Received /feedback command");
                                handleFeedbackCommand(String.valueOf(chatId));
                            } else if ("/write_feedback".equals(text)) {
                                System.out.println("Received /write_feedback command");
                                handleWriteFeedbackCommand(String.valueOf(chatId));
                            } else if (text.startsWith("/make_an_appointment")) {
                                System.out.println("Received /make_an_appointment command");
                                String[] commandParts = text.split(" ");
                                if (commandParts.length == 2) {
                                    String selectedDate = commandParts[1];
                                    handleSelectedDate(chatId, selectedDate);
                                } else {
                                    sendMessage(String.valueOf(chatId), "Неправильный формат команды. Используйте /make_an_appointment <дата>");
                                }
                            } else {
                                System.out.println("Unknown command: " + text);
                            }
                        }
                    }
                } else {
                    System.out.println("Failed to retrieve updates. Response Code: " + connection.getResponseCode());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPhotos(long chatId) {
        try {
            List<String> photoUrls = new ArrayList<>();
            photoUrls.add(photoUrl1);
            photoUrls.add(photoUrl2);
            photoUrls.add(photoUrl3);
            photoUrls.add(photoUrl4);

            for (String photoUrl : photoUrls) {
                sendPhoto(String.valueOf(chatId), photoUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(String.valueOf(chatId), "Произошла ошибка при отправке фотографий.");
        }
    }

    private void sendPhoto(String chatId, String photoUrl) {
        String url = baseUrl + "/sendPhoto";
        String requestBody = "chat_id=" + chatId + "&photo=" + photoUrl;

        try {
            URL urlObject = new URL(url);

            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                System.out.println("Response Code: " + connection.getResponseCode());
                System.out.println("Response Body: " + response.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeStorage() {
        storage = StorageOptions.getDefaultInstance().getService();
    }

    private void uploadPhotosToGCS() {
        String localPhotosDirectory = "photos";
        File[] photoFiles = new File(localPhotosDirectory).listFiles();

        if (photoFiles != null) {
            for (File photoFile : photoFiles) {
                String objectName = "photos/" + photoFile.getName();
                byte[] photoBytes = readBytesFromFile(photoFile);

                BlobId blobId = BlobId.of(bucketName, objectName);
                Blob blob = storage.create(BlobInfo.newBuilder(blobId)
                        .setContentType("image/jpeg")
                        .build(), photoBytes);

                System.out.println("Photo uploaded to GCS: " + objectName);
            }
        } else {
            System.out.println("No photos found in the local directory.");
        }
    }

    private byte[] readBytesFromFile(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fileInputStream.read(bytes);
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }


    private List<String> getPhotoUrlsFromGCS() {
        List<String> photoUrls = new ArrayList<>();
        photoUrls.add(photoUrl1);
        photoUrls.add(photoUrl2);
        photoUrls.add(photoUrl3);
        photoUrls.add(photoUrl4);

        for (String photoUrl : photoUrls) {
            System.out.println("Photo URL from GCS: " + photoUrl);
        }

        return photoUrls;
    }

    private void sendStartMessage(long chatId) {
        sendMessage(String.valueOf(chatId), "Добрый день! Я Ева\n" +
                "Я мастер маникюра. Сделаю Вам красивые ногти с оригинальным дизайном. " +
                "Гарантирую приятную атмосферу, стерильные инструменты, одноразовые расходники. " +
                "Жду Вас у себя в студии. \nА пока Вы можете ознакомиться с командами ниже." +
                "\nХорошего дня!" +
                "\n" +
                "\n/photos - Фотографии работ" +
                "\n/make_an_appointment - Записаться на маникюр" +
                "\n/price - Цены" +
                "\n/feedback - Отзывы" +
                "\n/write_feedback - Написать отзыв");
    }

    private void sendHelpMessage(long chatId) {
        sendMessage(String.valueOf(chatId), "Если у Вас возникли вопросы, " +
                "вы можете обратиться прямо к мастеру маникюра Еве " +
                "в телеграме, нажав по ссылке ниже. \nХорошего дня!" +
                "\nhttps://t.me/evaaasik");
    }

    private void sendMessage(String chatId, String text) {
        try {
            String url = baseUrl + "/sendMessage";
            String requestBody = "chat_id=" + chatId + "&text=" + URLEncoder.encode(text, String.valueOf(StandardCharsets.UTF_8));

            URL urlObject = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setDoOutput(true);

            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                dos.writeBytes(requestBody);
                dos.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    System.out.println("Response Body: " + response.toString());
                }
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.out.println("Error Response: " + errorResponse.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void handleMakeAppointmentCommand(long chatId) {
        sendMessage(String.valueOf(chatId), "Выберите дату для записи на маникюр:");

        List<LocalDate> availableDates = appointmentService.getAvailableDates();

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        for (LocalDate date : availableDates) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(date.toString()));
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);

        sendReplyKeyboard(chatId, "Выберите дату:", keyboardMarkup);
    }

    public void handleSelectedDate(long chatId, String selectedDate) {

        LocalDate date = LocalDate.parse(selectedDate);


        List<String> availableTimes = appointmentService.getAvailableTimes(date);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        for (String time : availableTimes) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(time));
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);


        sendReplyKeyboard(chatId, "Выберите время:", keyboardMarkup);
    }


    public void handleSelectedTime(long chatId, String selectedTime, User user) {

        LocalTime time = LocalTime.parse(selectedTime);


        boolean success = appointmentService.makeAppointment(user, LocalDate.now(), time);

        if (success) {
            sendMessage(String.valueOf(chatId), "Вы успешно записаны на маникюр!");
        } else {
            sendMessage(String.valueOf(chatId), "Извините, выбранное время уже занято. Пожалуйста, выберите другое время.");
        }
    }
    private void sendReplyKeyboard(long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);


        execute(message);
    }

    private void execute(SendMessage message) {
        String url = baseUrl + "/sendMessage";
        String requestBody = "chat_id=" + message.getChatId() + "&text=" + message.getText();

        try {
            URL urlObject = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                byte[] input = requestBody.getBytes("utf-8");
                dos.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    System.out.println("Response Body: " + response.toString());
                }
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.out.println("Error Response: " + errorResponse.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void sendPriceMessage(long chatId) {
        String messageForPrice = "<b>-> Комплекс:</b> Снятие + маникюр + покрытие гель лаком + уход 1700 рублей.\n\n" +
                "-> Маникюр комбинированный <b>500</b> рублей.\n" +
                "-> Спа для рук <b>300</b> рублей.\n" +
                "-> Фрэнч <b>400</b> рублей.\n" +
                "-> Дизайн от <b>50</b> рублей за ноготь.";
        sendMessageWithHTML(String.valueOf(chatId), messageForPrice);
    }

    private void sendMessageWithHTML(String chatId, String htmlMessage) {
        try {
            String url = baseUrl + "/sendMessage";
            String escapedHtmlMessage = URLEncoder.encode(htmlMessage, String.valueOf(StandardCharsets.UTF_8));
            String requestBody = "chat_id=" + chatId + "&text=" + escapedHtmlMessage + "&parse_mode=HTML";

            URL urlObject = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            try (InputStream is = connection.getInputStream()) {
                // Просто прочитаем весь поток в строку, чтобы избежать ошибок
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    System.out.println("Response Body: " + response.toString());
                }
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @PostMapping("/write_feedback")
    public void handleWriteFeedbackCommand(@RequestBody String requestBody) {
        long chatId = extractChatId(requestBody);

        if (chatId != 0) {
            String welcomeMessage = "Спасибо, что хотите оставить отзыв! Пожалуйста, напишите свой отзыв:";
            sendMessageWithHTML(String.valueOf(chatId), welcomeMessage);
        } else {
            System.out.println("Chat ID not found or invalid");
        }
    }


    @PostMapping("/get")
    public void handleFeedbackCommand(@RequestBody String requestBody) {
        long chatId = extractChatId(requestBody);
        List<Feedback> feedbackList = feedbackService.getFeedbackByChatId(chatId);

        StringBuilder message = new StringBuilder("<b>Отзывы:</b>\n\n");
        for (Feedback feedback : feedbackList) {
            message.append("<strong>").append(feedback.getUsername()).append("</strong>: ")
                    .append(feedback.getMessage()).append("\n");
        }

        sendMessageWithHTML(String.valueOf(chatId), message.toString());
    }

    @PostMapping("/save")
    public void saveFeedback(@RequestBody String requestBody) {
        long chatId = extractChatId(requestBody);
        String username = extractUsername(requestBody);
        String message = extractMessage(requestBody);

        Feedback feedback = new Feedback();
        feedback.setChatId(chatId);
        feedback.setUsername(username);
        feedback.setMessage(message);

        feedbackService.saveFeedback(feedback);
    }

    private long extractChatId(String requestBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            JsonNode messageNode = jsonNode.get("message");


            if (messageNode != null) {
                JsonNode chatNode = messageNode.get("chat");


                if (chatNode != null && chatNode.has("id") && chatNode.get("id").isNumber()) {
                    return chatNode.get("id").asLong();
                }
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }

        return 0;
    }

    private String extractUsername(String requestBody) {

        return "";
    }

    private String extractMessage(String requestBody) {

        return "";
    }

    private void handleExecuteError(IOException e) {
        e.printStackTrace();
    }

}



