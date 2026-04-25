package edu.language.kbee.service.impl;

import edu.language.kbee.enums.DictationTopicStatus;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.DictationTopic;
import edu.language.kbee.payload.DictationTopicDto;
import edu.language.kbee.payload.request.DictationTopicRequest;
import edu.language.kbee.repository.DictationTopicRepository;
import edu.language.kbee.service.DictationTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictationTopicServiceImpl implements DictationTopicService {

    private final DictationTopicRepository dictationTopicRepository;

    @Override
    public List<DictationTopicDto> getAllTopics() {
        return dictationTopicRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<DictationTopicDto> getPublishedTopics() {
        return dictationTopicRepository.findAll().stream()
                .filter(topic -> topic.getStatus() == DictationTopicStatus.PUBLISHED)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public DictationTopicDto getTopicById(String id) {
        DictationTopic topic = dictationTopicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DictationTopic not found with id: " + id));
        return mapToDto(topic);
    }

    @Override
    @Transactional
    public DictationTopicDto createTopic(DictationTopicRequest request) {
        DictationTopic topic = DictationTopic.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .icon(request.getIcon() != null ? request.getIcon() : "📺")
                .status(DictationTopicStatus.DRAFT)
                .lessonCount(0)
                .build();

        DictationTopic savedTopic = dictationTopicRepository.save(topic);
        return mapToDto(savedTopic);
    }

    @Override
    @Transactional
    public DictationTopicDto updateTopic(String id, DictationTopicRequest request) {
        DictationTopic topic = dictationTopicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DictationTopic not found with id: " + id));

        topic.setTitle(request.getTitle());
        topic.setDescription(request.getDescription());
        if (request.getIcon() != null) {
            topic.setIcon(request.getIcon());
        }

        DictationTopic updatedTopic = dictationTopicRepository.save(topic);
        return mapToDto(updatedTopic);
    }

    @Override
    @Transactional
    public void deleteTopic(String id) {
        DictationTopic topic = dictationTopicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DictationTopic not found with id: " + id));
        dictationTopicRepository.delete(topic);
    }

    @Override
    @Transactional
    public DictationTopicDto toggleStatus(String id) {
        DictationTopic topic = dictationTopicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DictationTopic not found with id: " + id));

        topic.setStatus(topic.getStatus() == DictationTopicStatus.PUBLISHED 
                ? DictationTopicStatus.DRAFT 
                : DictationTopicStatus.PUBLISHED);

        DictationTopic updatedTopic = dictationTopicRepository.save(topic);
        return mapToDto(updatedTopic);
    }

    private DictationTopicDto mapToDto(DictationTopic topic) {
        return DictationTopicDto.builder()
                .id(topic.getId())
                .title(topic.getTitle())
                .description(topic.getDescription())
                .icon(topic.getIcon())
                .status(topic.getStatus())
                .lessonCount(topic.getLessonCount())
                .build();
    }
}
