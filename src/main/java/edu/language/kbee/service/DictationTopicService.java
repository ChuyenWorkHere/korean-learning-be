package edu.language.kbee.service;

import edu.language.kbee.payload.DictationTopicDto;
import edu.language.kbee.payload.request.DictationTopicRequest;

import java.util.List;

public interface DictationTopicService {
    List<DictationTopicDto> getAllTopics();
    List<DictationTopicDto> getPublishedTopics();
    DictationTopicDto getTopicById(String id);
    DictationTopicDto createTopic(DictationTopicRequest request);
    DictationTopicDto updateTopic(String id, DictationTopicRequest request);
    void deleteTopic(String id);
    DictationTopicDto toggleStatus(String id);
}
