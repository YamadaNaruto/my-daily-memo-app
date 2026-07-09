package org.example.mydailymemoapp;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import nl.martijndwars.webpush.PushService;

import nl.martijndwars.webpush.Notification;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Controller
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;
    private final UserService userService;
    private final PushSubscriptionRepository pushSubscriptionRepository;

    @Value("${vapid.public.key}")
    private String publicKey;
    @Value("${vapid.private.key}")
    private String privateKey;


    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }
    @GetMapping("/register")
    public String registerForm(){
        return "register";
    }
    @PostMapping("/register")
    public String register(@ModelAttribute User user) {
        System.out.println("register呼び出し");
        userService.register(user.getUsername(), user.getPassword());
        return "redirect:/login";
    }
    @GetMapping("/home")
    public  String home(Model model){
        LocalDate today = LocalDate.now();
        Diary todayDiary = diaryService.findByDate(today);
        model.addAttribute("todayDiary",todayDiary );
        model.addAttribute("today",today);
        return "home";
    }

    @PostMapping("/upload")
    public  String upload(
            @RequestParam("image") MultipartFile file,
            @RequestParam("content") String content,
            @RequestParam("title") String title

    ) throws IOException {
        //imageを保存し、日記のテキストと、タイトルもここで保存する
        //uploadフォルダがなければ作成
        String imagePath = null;
        File uploadDir = new File("uploads");
        if (!uploadDir.exists()){
            uploadDir.mkdirs();
        }
        String originalFilename = file.getOriginalFilename();
        String extension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                :"";
        String newFilename = UUID.randomUUID().toString() + extension;

        if(!file.isEmpty()) {
            imagePath = "uploads/" + newFilename;
            Files.copy(file.getInputStream(), Path.of(imagePath));
        }

        diaryService.save(title, content, imagePath);
        return "redirect:/home";

    }
    @GetMapping("/history")
    public String history( @RequestParam(required = false) Integer year,
                           @RequestParam(required = false)Integer month,
                           Model model){
        if (year == null) year = LocalDate.now().getYear();
        if (month == null) month = LocalDate.now().getMonthValue();

        LocalDate today = LocalDate.now();
        List<Diary> diaries = diaryService.findByYearAndMonth(year,month);
        model.addAttribute("diaries",diaries);
        model.addAttribute("today",today);
        model.addAttribute("selectedYear",year);
        model.addAttribute("selectedMonth",month);
        model.addAttribute("years",List.of(2024,2025,2026));
        model.addAttribute("months",List.of(1,2,3,4,5,6,7,8,9,10,11,12));
        return  "history";

    }

    @GetMapping("/diary/{id}")
    public String showDiary(@PathVariable Long id, Model model){
        Diary diary = diaryService.findById(id);
        model.addAttribute("diary",diary);
        return "diary/detail";
    }

    @PostMapping("/subscribe")
    @ResponseBody
    public void subscribe(@RequestBody Map<String,Object> body) {
        String endpoint = (String) body.get("endpoint");
        if (pushSubscriptionRepository.findByEndpoint(endpoint).isPresent()) {
            return; // すでに登録済みなら何もしない
        }
        //DBに購読情報を保存
        PushSubscription sub = new PushSubscription();
        sub.setEndpoint((String) body.get("endpoint"));
        Map<String,String> keys = (Map<String, String>) body.get("keys");
        sub.setP256dh(keys.get("p256dh"));
        sub.setAuth(keys.get("auth"));
        pushSubscriptionRepository.save(sub);
    }

    @Scheduled(cron = "0 30 8  * * *")
    public void sendPushNotification() throws GeneralSecurityException, JoseException, IOException, ExecutionException, InterruptedException {
        System.out.println("通知処理開始");
        Security.addProvider(new BouncyCastleProvider());
        //DBから購読情報を取得してプッシュ通知を送信
        PushService pushService = new PushService(publicKey,privateKey);
        List<PushSubscription> subs = pushSubscriptionRepository.findAll();
        for (PushSubscription sub : subs) {
            Notification notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    (sub.getAuth()),
                    "{\"title\":\"日記\",\"body\":\"日記を書きましょう\"}"
            );
            pushService.send(notification);
            System.out.println("通知送信完了: " + sub.getEndpoint());
        }
    }
    @GetMapping("/vapidPublicKey")
    @ResponseBody
    public String vapidPublicKey(){
        return publicKey;
    }
}
