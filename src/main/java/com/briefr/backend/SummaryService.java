package com.briefr.backend;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final StandupEntryRepository repo;

    @Value("${groq.api.key}")
    private String groqKey;

    @Value("${slack.bot.token}")
    private String slackToken;

    @Value("${slack.standup.channel}")
    private String channel;

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(cron = "0 0 9 * * MON-FRI")
    public void generateAndPostSummary() {
        List<StandupEntry> entries = repo.findByStandupDate(LocalDate.now());
        if (entries.isEmpty()) return;

        String combined = entries.stream()
                .map(e -> e.getUserName() + ": " + e.getMessage())
                .collect(Collectors.joining("\n"));

        String summary = callGroq(combined);
        postToSlack(summary);
    }

    public String callGroq(String standups) {
        String prompt = """
                You are a technical team lead. Below are today's standup updates from the team.
                Produce a 4-5 line summary highlighting:
                - What the team is working on today
                - Any blockers or risks
                - Any dependencies between team members
                Keep it concise and actionable.
                
                Standups:
                """ + standups;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqKey);

        Map<String, Object> body = Map.of(
                "model", "llama3-8b-8192",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 300
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map response = restTemplate.postForObject(
                "https://api.groq.com/openai/v1/chat/completions",
                request, Map.class);

        List<Map> choices = (List<Map>) response.get("choices");
        Map message = (Map) choices.get(0).get("message");
        return (String) message.get("content");
    }

    public void postToSlack(String summary) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(slackToken);

        Map<String, Object> body = Map.of(
                "channel", channel,
                "text", "📋 *Team Standup Summary*\n\n" + summary
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForObject("https://slack.com/api/chat.postMessage", request, Map.class);
    }
}