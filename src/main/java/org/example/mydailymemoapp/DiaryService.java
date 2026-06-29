package org.example.mydailymemoapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// DiaryService.java
@Service

public class DiaryService {
    private  final DiaryRepository diaryRepository;
    public DiaryService(DiaryRepository diaryRepository) {
        this.diaryRepository = diaryRepository;
    }    public void save(String title, String content, String imagePath) {
        Diary diary = new Diary();
        diary.setTitle(title);
        diary.setContent(content);
        diary.setImagePath(imagePath);
        diaryRepository.save(diary);
    }

    public List<Diary> findAllOrderByDateDesc() {
        return diaryRepository.findAllByOrderByCreatedAtDesc();
    }
    public Diary findByDate(LocalDate date){
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return diaryRepository.findFirstByCreatedAtBetween(start,end).orElse(null);
    }


    public Diary findById(Long id) {
        return diaryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("日記が見つかりません"));
    }
    public List<Diary> findByYearAndMonth(int year, int month) {
        LocalDateTime start = LocalDate.of(year,month,1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        return diaryRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start,end);

    }
}