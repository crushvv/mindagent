package com.mindagent.chat.api;

import com.mindagent.chat.core.SpeechToTextService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/transcribe")
public class TranscribeController {

    private final SpeechToTextService speechToTextService;

    public TranscribeController(SpeechToTextService speechToTextService) {
        this.speechToTextService = speechToTextService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TranscribeResponse transcribe(@RequestPart("file") MultipartFile file) {
        try {
            String transcript = speechToTextService.transcribe(file.getBytes(), file.getOriginalFilename());
            return new TranscribeResponse(transcript, file.getOriginalFilename(), "MULTIPART_FILE");
        } catch (Exception ex) {
            return new TranscribeResponse("WHISPER_TRANSCRIBE_FAILED", file.getOriginalFilename(), "MULTIPART_FILE");
        }
    }
}
