package edu.language.kbee.service;

import tools.jackson.databind.JsonNode;

public interface AssemblyAiService {

    JsonNode generateTranscript(String audioUrl) throws Exception;

}
