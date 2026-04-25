package edu.language.kbee.service.impl;

import edu.language.kbee.enums.DictationLessonStatus;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.DictationLesson;
import edu.language.kbee.model.DictationSentence;
import edu.language.kbee.model.DictationTopic;
import edu.language.kbee.payload.DictationLessonDto;
import edu.language.kbee.payload.DictationSentenceDto;
import edu.language.kbee.payload.request.DictationLessonRequest;
import edu.language.kbee.repository.DictationLessonRepository;
import edu.language.kbee.repository.DictationTopicRepository;
import edu.language.kbee.service.DictationLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictationLessonServiceImpl implements DictationLessonService {

    private final DictationLessonRepository dictationLessonRepository;
    private final DictationTopicRepository dictationTopicRepository;

    @Override
    public List<DictationLessonDto> getLessonsByTopic(String topicId) {
        DictationTopic topic = dictationTopicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("DictationTopic not found with id: " + topicId));
        return dictationLessonRepository.findByTopicIdOrderByCreatedDateAsc(topicId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public DictationLessonDto getLessonById(String lessonId) {
        DictationLesson lesson = dictationLessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("DictationLesson not found with id: " + lessonId));
        return mapToDto(lesson);
    }

    @Override
    @Transactional
    public DictationLessonDto createLesson(String topicId, DictationLessonRequest request) {
        DictationTopic topic = dictationTopicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("DictationTopic not found with id: " + topicId));

        DictationLesson lesson = new DictationLesson();
        lesson.setTitle(request.getTitle());
        lesson.setMediaType(request.getMediaType());
        lesson.setTopic(topic);
        
        if (request.getYoutubeId() != null) lesson.setYoutubeId(request.getYoutubeId());
        if (request.getAudioUrl() != null) lesson.setAudioUrl(request.getAudioUrl());
        if (request.getDuration() != null) lesson.setDuration(request.getDuration());
        if (request.getStatus() != null) lesson.setStatus(request.getStatus());

        DictationLesson savedLesson = dictationLessonRepository.save(lesson);
        
        // Update topic lesson count
        topic.setLessonCount(topic.getLessonCount() + 1);
        dictationTopicRepository.save(topic);

        return mapToDto(savedLesson);
    }

    @Override
    @Transactional
    public DictationLessonDto updateLesson(String lessonId, DictationLessonRequest request) {
        DictationLesson lesson = dictationLessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("DictationLesson not found with id: " + lessonId));

        lesson.setTitle(request.getTitle());
        lesson.setMediaType(request.getMediaType());
        lesson.setYoutubeId(request.getYoutubeId());
        lesson.setAudioUrl(request.getAudioUrl());
        lesson.setDuration(request.getDuration() != null ? request.getDuration() : "--:--");
        if (request.getStatus() != null) {
            lesson.setStatus(request.getStatus());
        }

        // Handle sentences
        lesson.getSentences().clear();
        if (request.getSentences() != null) {
            for (DictationSentenceDto sentenceDto : request.getSentences()) {
                DictationSentence sentence = new DictationSentence();
                sentence.setOrderIndex(sentenceDto.getOrderIndex());
                sentence.setStartTime(sentenceDto.getStartTime());
                sentence.setEndTime(sentenceDto.getEndTime());
                sentence.setText(sentenceDto.getText());
                sentence.setTranslation(sentenceDto.getTranslation());
                sentence.setLesson(lesson);
                lesson.getSentences().add(sentence);
            }
        }
        
        lesson.setSentenceCount(lesson.getSentences().size());

        return mapToDto(dictationLessonRepository.save(lesson));
    }

    @Override
    @Transactional
    public void deleteLesson(String lessonId) {
        DictationLesson lesson = dictationLessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("DictationLesson not found with id: " + lessonId));
        
        DictationTopic topic = lesson.getTopic();
        topic.setLessonCount(Math.max(0, topic.getLessonCount() - 1));
        dictationTopicRepository.save(topic);

        dictationLessonRepository.delete(lesson);
    }

    @Override
    @Transactional
    public DictationLessonDto toggleStatus(String lessonId) {
        DictationLesson lesson = dictationLessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("DictationLesson not found with id: " + lessonId));

        lesson.setStatus(lesson.getStatus() == DictationLessonStatus.PUBLISHED
                ? DictationLessonStatus.DRAFT
                : DictationLessonStatus.PUBLISHED);

        return mapToDto(dictationLessonRepository.save(lesson));
    }

    private DictationLessonDto mapToDto(DictationLesson lesson) {
        List<DictationSentenceDto> sentenceDtos = new ArrayList<>();
        if (lesson.getSentences() != null) {
            sentenceDtos = lesson.getSentences().stream()
                    .map(s -> DictationSentenceDto.builder()
                            .id(s.getId())
                            .orderIndex(s.getOrderIndex())
                            .startTime(s.getStartTime())
                            .endTime(s.getEndTime())
                            .text(s.getText())
                            .translation(s.getTranslation())
                            .build())
                    .collect(Collectors.toList());
        }

        return DictationLessonDto.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .mediaType(lesson.getMediaType())
                .youtubeId(lesson.getYoutubeId())
                .audioUrl(lesson.getAudioUrl())
                .sentenceCount(lesson.getSentenceCount())
                .status(lesson.getStatus())
                .duration(lesson.getDuration())
                .topicId(lesson.getTopic().getId())
                .sentences(sentenceDtos)
                .createdAt(lesson.getCreatedDate())
                .updatedAt(lesson.getLastModifiedDate())
                .build();
    }
}
